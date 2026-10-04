package com.ses.whodatidols.repository;

import com.ses.whodatidols.model.Actor;
import com.ses.whodatidols.model.CastMemberDto;
import com.ses.whodatidols.service.TmdbService;
import com.ses.whodatidols.service.TvMazeService;
import com.ses.whodatidols.util.ImageUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BooleanSupplier;

@Repository
@SuppressWarnings({"SqlResolve", "SqlNoDataSourceInspection"})
public class ActorRepository {

    public interface PhotoSyncProgressCallback {
        void onProgress(int current, int total, String actorName);
    }

    private final JdbcTemplate jdbcTemplate;
    private final TmdbService tmdbService;
    private final TvMazeService tvMazeService;

    @Value("${media.actor.images.path:D:\\SourceFiles\\mssql\\media\\images\\actors}")
    private String actorImagesPath;

    public ActorRepository(JdbcTemplate jdbcTemplate,
                           @Lazy TmdbService tmdbService,
                           @Lazy TvMazeService tvMazeService) {
        this.jdbcTemplate = jdbcTemplate;
        this.tmdbService = tmdbService;
        this.tvMazeService = tvMazeService;
        ensureSchema();
    }

    private void ensureSchema() {
        try {
            // 1. Actor Table
            jdbcTemplate.execute(
                    "IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Actor') " +
                    "BEGIN " +
                    "    CREATE TABLE [dbo].[Actor] ( " +
                    "        [ID] UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(), " +
                    "        [Name] NVARCHAR(255) NOT NULL, " +
                    "        [PhotoUrl] NVARCHAR(1000) NULL, " +
                    "        [RemotePhotoUrl] NVARCHAR(1000) NULL, " +
                    "        [TmdbId] INT NULL, " +
                    "        [TvMazeId] INT NULL, " +
                    "        [CreatedAt] DATETIME2 DEFAULT GETDATE() " +
                    "    ); " +
                    "END");

            // Migration: Add RemotePhotoUrl if missing
            jdbcTemplate.execute(
                    "IF NOT EXISTS (SELECT * FROM sys.columns WHERE Name = 'RemotePhotoUrl' AND Object_ID = OBJECT_ID('Actor')) " +
                    "BEGIN " +
                    "    ALTER TABLE [dbo].[Actor] ADD [RemotePhotoUrl] NVARCHAR(1000) NULL; " +
                    "END");

            // Migration: Add Biography, Birthday, Deathday, PlaceOfBirth, KnownFor, Gender if missing
            jdbcTemplate.execute(
                    "IF NOT EXISTS (SELECT * FROM sys.columns WHERE Name = 'Biography' AND Object_ID = OBJECT_ID('Actor')) " +
                    "BEGIN " +
                    "    ALTER TABLE [dbo].[Actor] ADD [Biography] NVARCHAR(MAX) NULL; " +
                    "END");

            jdbcTemplate.execute(
                    "IF NOT EXISTS (SELECT * FROM sys.columns WHERE Name = 'Birthday' AND Object_ID = OBJECT_ID('Actor')) " +
                    "BEGIN " +
                    "    ALTER TABLE [dbo].[Actor] ADD [Birthday] NVARCHAR(50) NULL; " +
                    "END");

            jdbcTemplate.execute(
                    "IF NOT EXISTS (SELECT * FROM sys.columns WHERE Name = 'Deathday' AND Object_ID = OBJECT_ID('Actor')) " +
                    "BEGIN " +
                    "    ALTER TABLE [dbo].[Actor] ADD [Deathday] NVARCHAR(50) NULL; " +
                    "END");

            jdbcTemplate.execute(
                    "IF NOT EXISTS (SELECT * FROM sys.columns WHERE Name = 'PlaceOfBirth' AND Object_ID = OBJECT_ID('Actor')) " +
                    "BEGIN " +
                    "    ALTER TABLE [dbo].[Actor] ADD [PlaceOfBirth] NVARCHAR(255) NULL; " +
                    "END");

            jdbcTemplate.execute(
                    "IF NOT EXISTS (SELECT * FROM sys.columns WHERE Name = 'KnownFor' AND Object_ID = OBJECT_ID('Actor')) " +
                    "BEGIN " +
                    "    ALTER TABLE [dbo].[Actor] ADD [KnownFor] NVARCHAR(100) NULL; " +
                    "END");

            jdbcTemplate.execute(
                    "IF NOT EXISTS (SELECT * FROM sys.columns WHERE Name = 'Gender' AND Object_ID = OBJECT_ID('Actor')) " +
                    "BEGIN " +
                    "    ALTER TABLE [dbo].[Actor] ADD [Gender] INT NULL; " +
                    "END");

            // 2. MovieActors Table
            jdbcTemplate.execute(
                    "IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'MovieActors') " +
                    "BEGIN " +
                    "    CREATE TABLE [dbo].[MovieActors] ( " +
                    "        [MovieID] UNIQUEIDENTIFIER NOT NULL, " +
                    "        [ActorID] UNIQUEIDENTIFIER NOT NULL, " +
                    "        [CharacterName] NVARCHAR(255) NULL, " +
                    "        [OrderIndex] INT DEFAULT 0 NOT NULL, " +
                    "        CONSTRAINT PK_MovieActors PRIMARY KEY ([MovieID], [ActorID]), " +
                    "        CONSTRAINT FK_MovieActors_Movie FOREIGN KEY ([MovieID]) REFERENCES [dbo].[Movie]([ID]) ON DELETE CASCADE, " +
                    "        CONSTRAINT FK_MovieActors_Actor FOREIGN KEY ([ActorID]) REFERENCES [dbo].[Actor]([ID]) ON DELETE CASCADE " +
                    "    ); " +
                    "END");

            // 3. SeriesActors Table
            jdbcTemplate.execute(
                    "IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'SeriesActors') " +
                    "BEGIN " +
                    "    CREATE TABLE [dbo].[SeriesActors] ( " +
                    "        [SeriesID] UNIQUEIDENTIFIER NOT NULL, " +
                    "        [ActorID] UNIQUEIDENTIFIER NOT NULL, " +
                    "        [CharacterName] NVARCHAR(255) NULL, " +
                    "        [OrderIndex] INT DEFAULT 0 NOT NULL, " +
                    "        CONSTRAINT PK_SeriesActors PRIMARY KEY ([SeriesID], [ActorID]), " +
                    "        CONSTRAINT FK_SeriesActors_Series FOREIGN KEY ([SeriesID]) REFERENCES [dbo].[Series]([ID]) ON DELETE CASCADE, " +
                    "        CONSTRAINT FK_SeriesActors_Actor FOREIGN KEY ([ActorID]) REFERENCES [dbo].[Actor]([ID]) ON DELETE CASCADE " +
                    "    ); " +
                    "END");

            // Indexes
            jdbcTemplate.execute(
                    "IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_actor_name' AND object_id = OBJECT_ID('Actor')) " +
                    "BEGIN " +
                    "    CREATE NONCLUSTERED INDEX idx_actor_name ON [dbo].[Actor]([Name]); " +
                    "END");

            jdbcTemplate.execute(
                    "IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_actor_tmdb' AND object_id = OBJECT_ID('Actor')) " +
                    "BEGIN " +
                    "    CREATE NONCLUSTERED INDEX idx_actor_tmdb ON [dbo].[Actor]([TmdbId]) WHERE [TmdbId] IS NOT NULL; " +
                    "END");

            jdbcTemplate.execute(
                    "IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_actor_tvmaze' AND object_id = OBJECT_ID('Actor')) " +
                    "BEGIN " +
                    "    CREATE NONCLUSTERED INDEX idx_actor_tvmaze ON [dbo].[Actor]([TvMazeId]) WHERE [TvMazeId] IS NOT NULL; " +
                    "END");

            jdbcTemplate.execute(
                    "IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_movieactors_movieid' AND object_id = OBJECT_ID('MovieActors')) " +
                    "BEGIN " +
                    "    CREATE NONCLUSTERED INDEX idx_movieactors_movieid ON [dbo].[MovieActors]([MovieID]); " +
                    "END");

            jdbcTemplate.execute(
                    "IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_movieactors_actorid' AND object_id = OBJECT_ID('MovieActors')) " +
                    "BEGIN " +
                    "    CREATE NONCLUSTERED INDEX idx_movieactors_actorid ON [dbo].[MovieActors]([ActorID]); " +
                    "END");

            jdbcTemplate.execute(
                    "IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_seriesactors_seriesid' AND object_id = OBJECT_ID('SeriesActors')) " +
                    "BEGIN " +
                    "    CREATE NONCLUSTERED INDEX idx_seriesactors_seriesid ON [dbo].[SeriesActors]([SeriesID]); " +
                    "END");

            jdbcTemplate.execute(
                    "IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_seriesactors_actorid' AND object_id = OBJECT_ID('SeriesActors')) " +
                    "BEGIN " +
                    "    CREATE NONCLUSTERED INDEX idx_seriesactors_actorid ON [dbo].[SeriesActors]([ActorID]); " +
                    "END");

        } catch (Exception e) {
            System.err.println("ActorRepository schema update failed: " + e.getMessage());
        }
    }

    private final RowMapper<Actor> actorRowMapper = (rs, rowNum) -> {
        Actor a = new Actor();
        a.setId(UUID.fromString(rs.getString("ID")));
        a.setName(rs.getString("Name"));
        a.setPhotoUrl(rs.getString("PhotoUrl"));
        try {
            a.setRemotePhotoUrl(rs.getString("RemotePhotoUrl"));
        } catch (Exception ignored) {}
        int tmdb = rs.getInt("TmdbId");
        if (!rs.wasNull()) a.setTmdbId(tmdb);
        int tvmaze = rs.getInt("TvMazeId");
        if (!rs.wasNull()) a.setTvmazeId(tvmaze);
        try {
            a.setBiography(rs.getString("Biography"));
            a.setBirthday(rs.getString("Birthday"));
            a.setDeathday(rs.getString("Deathday"));
            a.setPlaceOfBirth(rs.getString("PlaceOfBirth"));
            a.setKnownFor(rs.getString("KnownFor"));
            int gender = rs.getInt("Gender");
            if (!rs.wasNull()) a.setGender(gender);
        } catch (Exception ignored) {}
        java.sql.Timestamp ts = rs.getTimestamp("CreatedAt");
        if (ts != null) a.setCreatedAt(ts.toInstant());
        return a;
    };

    private final RowMapper<CastMemberDto> castMemberRowMapper = (rs, rowNum) -> {
        CastMemberDto dto = new CastMemberDto();
        dto.setActorId(UUID.fromString(rs.getString("ActorId")));
        dto.setName(rs.getString("Name"));
        dto.setCharacterName(rs.getString("CharacterName"));
        dto.setPhotoUrl(rs.getString("PhotoUrl"));
        int tmdb = rs.getInt("TmdbId");
        if (!rs.wasNull()) dto.setTmdbId(tmdb);
        int tvmaze = rs.getInt("TvMazeId");
        if (!rs.wasNull()) dto.setTvmazeId(tvmaze);
        dto.setOrderIndex(rs.getInt("OrderIndex"));
        return dto;
    };

    public Actor findById(UUID actorId) {
        if (actorId == null) return null;
        try {
            List<Actor> list = jdbcTemplate.query(
                    "SELECT ID, Name, PhotoUrl, RemotePhotoUrl, TmdbId, TvMazeId, Biography, Birthday, Deathday, PlaceOfBirth, KnownFor, Gender, CreatedAt FROM [dbo].[Actor] WHERE ID = ?",
                    actorRowMapper, actorId.toString());
            return list.isEmpty() ? null : list.get(0);
        } catch (Exception e) {
            return null;
        }
    }

    public void updateActorTmdbId(UUID actorId, int tmdbId) {
        if (actorId == null || tmdbId <= 0) return;
        try {
            jdbcTemplate.update("UPDATE [dbo].[Actor] SET TmdbId = ? WHERE ID = ? AND (TmdbId IS NULL OR TmdbId <= 0)",
                    tmdbId, actorId.toString());
        } catch (Exception e) {
            System.err.println("Failed to update TmdbId for actor " + actorId + ": " + e.getMessage());
        }
    }

    public void updateActorDetails(UUID actorId, String biography, String birthday, String deathday, String placeOfBirth, String knownFor, Integer gender) {
        if (actorId == null) return;
        try {
            jdbcTemplate.update(
                    "UPDATE [dbo].[Actor] SET Biography = ?, Birthday = ?, Deathday = ?, PlaceOfBirth = ?, KnownFor = ?, Gender = ? WHERE ID = ?",
                    biography, birthday, deathday, placeOfBirth, knownFor, gender, actorId.toString());
        } catch (Exception e) {
            System.err.println("Failed to update actor details in DB: " + e.getMessage());
        }
    }

    public List<Map<String, Object>> getMoviesForActor(UUID actorId) {
        if (actorId == null) return Collections.emptyList();
        String sql = """
                SELECT m.ID, m.name, m.slug, m.ReleaseYear, m.DurationMinutes, m.Country,
                       ma.CharacterName, ma.OrderIndex,
                       (SELECT STRING_AGG(c.Name, ', ') FROM Categories c
                        JOIN MovieCategories mc ON mc.CategoryID = c.ID
                        WHERE mc.MovieID = m.ID) AS category
                FROM [dbo].[MovieActors] ma
                JOIN [dbo].[Movie] m ON ma.MovieID = m.ID
                WHERE ma.ActorID = ? AND m.IsHidden = 0
                ORDER BY m.ReleaseYear DESC, m.name ASC
                """;
        try {
            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", rs.getString("ID"));
                map.put("name", rs.getString("name"));
                map.put("slug", rs.getString("slug"));
                int year = rs.getInt("ReleaseYear");
                if (!rs.wasNull()) map.put("releaseYear", year);
                int dur = rs.getInt("DurationMinutes");
                if (!rs.wasNull()) map.put("durationMinutes", dur);
                map.put("country", rs.getString("Country"));
                map.put("characterName", rs.getString("CharacterName"));
                map.put("category", rs.getString("category"));
                map.put("posterUrl", "/media/image/" + rs.getString("ID"));
                map.put("type", "movie");
                return map;
            }, actorId.toString());
        } catch (Exception e) {
            System.err.println("Failed to get movies for actor " + actorId + ": " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<Map<String, Object>> getSeriesForActor(UUID actorId) {
        if (actorId == null) return Collections.emptyList();
        String sql = """
                SELECT s.ID, s.name, s.slug, s.Country, s.SeriesType, s.finalStatus,
                       sa.CharacterName, sa.OrderIndex,
                       (SELECT MIN(e.ReleaseYear) FROM Episode e WHERE e.SeriesId = s.ID AND e.IsHidden = 0) AS ReleaseYear,
                       (SELECT COUNT(*) FROM Episode e WHERE e.SeriesId = s.ID AND e.IsHidden = 0) AS episodeCount,
                       (SELECT STRING_AGG(c.Name, ', ') FROM Categories c
                        JOIN SeriesCategories sc ON sc.CategoryID = c.ID
                        WHERE sc.SeriesID = s.ID) AS category
                FROM [dbo].[SeriesActors] sa
                JOIN [dbo].[Series] s ON sa.SeriesID = s.ID
                WHERE sa.ActorID = ? AND s.IsHidden = 0
                ORDER BY s.name ASC
                """;
        try {
            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", rs.getString("ID"));
                map.put("name", rs.getString("name"));
                map.put("slug", rs.getString("slug"));
                int releaseYear = rs.getInt("ReleaseYear");
                if (!rs.wasNull()) map.put("releaseYear", releaseYear);
                map.put("country", rs.getString("Country"));
                map.put("seriesType", rs.getString("SeriesType"));
                map.put("finalStatus", rs.getInt("finalStatus"));
                map.put("characterName", rs.getString("CharacterName"));
                map.put("episodeCount", rs.getInt("episodeCount"));
                map.put("category", rs.getString("category"));
                map.put("posterUrl", "/media/image/" + rs.getString("ID"));
                map.put("type", "series");
                return map;
            }, actorId.toString());
        } catch (Exception e) {
            System.err.println("Failed to get series for actor " + actorId + ": " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public boolean hasLocalPhoto(UUID actorId) {
        if (actorId == null || actorImagesPath == null || actorImagesPath.trim().isEmpty()) {
            return false;
        }
        try {
            Path targetFile = Paths.get(actorImagesPath, actorId.toString() + ".jpg").normalize();
            return Files.exists(targetFile) && Files.size(targetFile) > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Downloads an actor's photo from a remote URL to the server's local actor image directory,
     * saving it as {actorId}.jpg.
     * Returns the local served endpoint (/media/actor/{actorId}) or the fallback URL on error.
     */
    public String saveActorPhotoLocally(UUID actorId, String remotePhotoUrl) {
        if (actorId == null || remotePhotoUrl == null || remotePhotoUrl.trim().isEmpty()) {
            return null;
        }
        String trimmed = remotePhotoUrl.trim();
        if (trimmed.startsWith("/media/actor/")) {
            return trimmed;
        }
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            return trimmed;
        }

        try {
            if (actorImagesPath == null || actorImagesPath.trim().isEmpty()) {
                return trimmed;
            }
            Path dir = Paths.get(actorImagesPath).toAbsolutePath().normalize();
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            Path targetFile = dir.resolve(actorId.toString() + ".jpg").normalize();
            if (!Files.exists(targetFile) || Files.size(targetFile) == 0) {
                ImageUtils.saveImageFromUrlAsJpg(trimmed, targetFile);
            }
            return "/media/actor/" + actorId;
        } catch (Exception e) {
            System.err.println("Could not download actor photo for " + actorId + " (" + trimmed + "): " + e.getMessage());
            return trimmed;
        }
    }

    /**
     * Restores an actor's missing photo on disk using RemotePhotoUrl, TMDB or TVMaze.
     * Returns true if the file now exists on disk.
     */
    public boolean restoreActorPhoto(UUID actorId) {
        if (actorId == null) return false;
        if (hasLocalPhoto(actorId)) return true;

        try {
            List<Actor> list = jdbcTemplate.query(
                    "SELECT ID, Name, PhotoUrl, RemotePhotoUrl, TmdbId, TvMazeId, CreatedAt FROM [dbo].[Actor] WHERE ID = ?",
                    actorRowMapper, actorId.toString());
            if (list.isEmpty()) return false;
            Actor a = list.get(0);

            // 1. Try RemotePhotoUrl
            String remote = a.getRemotePhotoUrl();
            if (remote != null && (remote.startsWith("http://") || remote.startsWith("https://"))) {
                saveActorPhotoLocally(actorId, remote);
                if (hasLocalPhoto(actorId)) {
                    jdbcTemplate.update("UPDATE [dbo].[Actor] SET PhotoUrl = ? WHERE ID = ?",
                            "/media/actor/" + actorId, actorId.toString());
                    return true;
                }
            }

            // 2. Try PhotoUrl if it contains http
            String photo = a.getPhotoUrl();
            if (photo != null && (photo.startsWith("http://") || photo.startsWith("https://"))) {
                saveActorPhotoLocally(actorId, photo);
                if (hasLocalPhoto(actorId)) {
                    jdbcTemplate.update("UPDATE [dbo].[Actor] SET RemotePhotoUrl = ?, PhotoUrl = ? WHERE ID = ?",
                            photo, "/media/actor/" + actorId, actorId.toString());
                    return true;
                }
            }

            // 3. Try TMDB by tmdbId
            if (a.getTmdbId() != null && a.getTmdbId() > 0 && tmdbService != null) {
                String profileUrl = tmdbService.getPersonProfileUrl(a.getTmdbId());
                if (profileUrl != null && !profileUrl.trim().isEmpty()) {
                    saveActorPhotoLocally(actorId, profileUrl);
                    if (hasLocalPhoto(actorId)) {
                        jdbcTemplate.update("UPDATE [dbo].[Actor] SET RemotePhotoUrl = ?, PhotoUrl = ? WHERE ID = ?",
                                profileUrl, "/media/actor/" + actorId, actorId.toString());
                        return true;
                    }
                }
            }

            // 4. Try TVMaze by tvmazeId
            if (a.getTvmazeId() != null && a.getTvmazeId() > 0 && tvMazeService != null) {
                String profileUrl = tvMazeService.getPersonImageUrl(a.getTvmazeId());
                if (profileUrl != null && !profileUrl.trim().isEmpty()) {
                    saveActorPhotoLocally(actorId, profileUrl);
                    if (hasLocalPhoto(actorId)) {
                        jdbcTemplate.update("UPDATE [dbo].[Actor] SET RemotePhotoUrl = ?, PhotoUrl = ? WHERE ID = ?",
                                profileUrl, "/media/actor/" + actorId, actorId.toString());
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to restore actor photo for " + actorId + ": " + e.getMessage());
        }
        return false;
    }

    /**
     * Checks if a movie has any cast member whose local photo file does not exist on disk.
     */
    public boolean isMovieMissingAnyPhoto(UUID movieId) {
        if (movieId == null) return false;
        List<String> actorIds = jdbcTemplate.queryForList(
                "SELECT CAST(ActorID AS NVARCHAR(36)) FROM [dbo].[MovieActors] WHERE MovieID = ?",
                String.class, movieId.toString());
        if (actorIds.isEmpty()) return true;
        for (String idStr : actorIds) {
            try {
                if (!hasLocalPhoto(UUID.fromString(idStr))) {
                    return true;
                }
            } catch (Exception ignored) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a series has any cast member whose local photo file does not exist on disk.
     */
    public boolean isSeriesMissingAnyPhoto(UUID seriesId) {
        if (seriesId == null) return false;
        List<String> actorIds = jdbcTemplate.queryForList(
                "SELECT CAST(ActorID AS NVARCHAR(36)) FROM [dbo].[SeriesActors] WHERE SeriesID = ?",
                String.class, seriesId.toString());
        if (actorIds.isEmpty()) return true;
        for (String idStr : actorIds) {
            try {
                if (!hasLocalPhoto(UUID.fromString(idStr))) {
                    return true;
                }
            } catch (Exception ignored) {
                return true;
            }
        }
        return false;
    }

    /**
     * Scans actors in the database who have a remote URL but are missing their photo on disk,
     * and re-downloads them with progress reporting.
     */
    public int downloadMissingLocalPhotos(PhotoSyncProgressCallback callback, BooleanSupplier shouldStop) {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT CAST(ID AS NVARCHAR(36)) AS ID, Name, PhotoUrl, RemotePhotoUrl FROM [dbo].[Actor] " +
                    "WHERE (RemotePhotoUrl IS NOT NULL AND RemotePhotoUrl LIKE 'http%') " +
                    "   OR (PhotoUrl LIKE 'http%')");

            // Filter to only those genuinely missing on disk first to determine real total
            List<Map<String, Object>> missingRows = new ArrayList<>();
            for (Map<String, Object> r : rows) {
                try {
                    UUID id = UUID.fromString((String) r.get("ID"));
                    if (!hasLocalPhoto(id)) {
                        missingRows.add(r);
                    }
                } catch (Exception ignored) {}
            }

            int totalMissing = missingRows.size();
            if (totalMissing == 0) {
                return 0;
            }

            if (callback != null) {
                callback.onProgress(0, totalMissing, "Fotograflar hazirlaniyor...");
            }

            int count = 0;
            int processed = 0;
            for (Map<String, Object> row : missingRows) {
                if (shouldStop != null && shouldStop.getAsBoolean()) {
                    break;
                }
                processed++;
                String name = (String) row.get("Name");
                if (callback != null) {
                    callback.onProgress(processed, totalMissing, name);
                }

                try {
                    UUID id = UUID.fromString((String) row.get("ID"));
                    String remoteUrl = (String) row.get("RemotePhotoUrl");
                    if (remoteUrl == null || !remoteUrl.startsWith("http")) {
                        remoteUrl = (String) row.get("PhotoUrl");
                    }
                    if (remoteUrl != null && remoteUrl.startsWith("http")) {
                        saveActorPhotoLocally(id, remoteUrl);
                        if (hasLocalPhoto(id)) {
                            jdbcTemplate.update(
                                    "UPDATE [dbo].[Actor] SET PhotoUrl = ?, RemotePhotoUrl = ? WHERE ID = ?",
                                    "/media/actor/" + id, remoteUrl, id.toString());
                            count++;
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error downloading missing photo: " + e.getMessage());
                }
            }
            return count;
        } catch (Exception e) {
            System.err.println("Failed to query missing actor photos: " + e.getMessage());
            return 0;
        }
    }

    public int downloadMissingLocalPhotos(BooleanSupplier shouldStop) {
        return downloadMissingLocalPhotos(null, shouldStop);
    }

    public int downloadMissingLocalPhotos() {
        return downloadMissingLocalPhotos(null, null);
    }

    @Transactional
    public UUID findOrCreateActor(String name, String photoUrl, Integer tmdbId, Integer tvmazeId) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        String trimmedName = name.trim();
        Actor existing = null;

        // 1. By TMDB ID
        if (tmdbId != null && tmdbId > 0) {
            List<Actor> list = jdbcTemplate.query(
                    "SELECT ID, Name, PhotoUrl, RemotePhotoUrl, TmdbId, TvMazeId, Biography, Birthday, Deathday, PlaceOfBirth, KnownFor, Gender, CreatedAt FROM [dbo].[Actor] WHERE TmdbId = ?",
                    actorRowMapper, tmdbId);
            if (!list.isEmpty()) {
                existing = list.get(0);
            }
        }

        // 2. By TVMaze ID
        if (existing == null && tvmazeId != null && tvmazeId > 0) {
            List<Actor> list = jdbcTemplate.query(
                    "SELECT ID, Name, PhotoUrl, RemotePhotoUrl, TmdbId, TvMazeId, Biography, Birthday, Deathday, PlaceOfBirth, KnownFor, Gender, CreatedAt FROM [dbo].[Actor] WHERE TvMazeId = ?",
                    actorRowMapper, tvmazeId);
            if (!list.isEmpty()) {
                existing = list.get(0);
            }
        }

        // 3. By Name (case insensitive)
        if (existing == null) {
            List<Actor> list = jdbcTemplate.query(
                    "SELECT ID, Name, PhotoUrl, RemotePhotoUrl, TmdbId, TvMazeId, Biography, Birthday, Deathday, PlaceOfBirth, KnownFor, Gender, CreatedAt FROM [dbo].[Actor] WHERE LOWER(TRIM(Name)) = LOWER(?)",
                    actorRowMapper, trimmedName);
            if (!list.isEmpty()) {
                existing = list.get(0);
            }
        }

        if (existing != null) {
            boolean updated = false;
            String currentPhoto = existing.getPhotoUrl();
            String currentRemote = existing.getRemotePhotoUrl();

            String newRemote = (photoUrl != null && (photoUrl.startsWith("http://") || photoUrl.startsWith("https://")))
                    ? photoUrl.trim() : null;

            if (newRemote != null && !newRemote.equals(currentRemote)) {
                existing.setRemotePhotoUrl(newRemote);
                currentRemote = newRemote;
                updated = true;
            }

            // Check if local file exists on disk
            if (!hasLocalPhoto(existing.getId())) {
                String toDownload = (newRemote != null) ? newRemote : currentRemote;
                if (toDownload == null && currentPhoto != null && (currentPhoto.startsWith("http://") || currentPhoto.startsWith("https://"))) {
                    toDownload = currentPhoto;
                }
                if (toDownload != null) {
                    saveActorPhotoLocally(existing.getId(), toDownload);
                    existing.setPhotoUrl("/media/actor/" + existing.getId());
                    if (existing.getRemotePhotoUrl() == null) {
                        existing.setRemotePhotoUrl(toDownload);
                    }
                    updated = true;
                }
            } else if (currentPhoto == null || currentPhoto.startsWith("http://") || currentPhoto.startsWith("https://")) {
                existing.setPhotoUrl("/media/actor/" + existing.getId());
                updated = true;
            }

            if (existing.getTmdbId() == null && tmdbId != null && tmdbId > 0) {
                existing.setTmdbId(tmdbId);
                updated = true;
            }
            if (existing.getTvmazeId() == null && tvmazeId != null && tvmazeId > 0) {
                existing.setTvmazeId(tvmazeId);
                updated = true;
            }
            if (updated) {
                jdbcTemplate.update(
                        "UPDATE [dbo].[Actor] SET PhotoUrl = ?, RemotePhotoUrl = ?, TmdbId = ?, TvMazeId = ? WHERE ID = ?",
                        existing.getPhotoUrl(), existing.getRemotePhotoUrl(), existing.getTmdbId(), existing.getTvmazeId(), existing.getId().toString());
            }
            return existing.getId();
        }

        UUID actorId = UUID.randomUUID();
        String remoteUrl = (photoUrl != null && (photoUrl.startsWith("http://") || photoUrl.startsWith("https://"))) ? photoUrl.trim() : null;
        String localPhotoUrl = null;
        if (remoteUrl != null) {
            saveActorPhotoLocally(actorId, remoteUrl);
            localPhotoUrl = "/media/actor/" + actorId;
        }

        jdbcTemplate.update(
                "INSERT INTO [dbo].[Actor] (ID, Name, PhotoUrl, RemotePhotoUrl, TmdbId, TvMazeId, CreatedAt) VALUES (?, ?, ?, ?, ?, ?, GETDATE())",
                actorId.toString(), trimmedName, localPhotoUrl, remoteUrl, tmdbId, tvmazeId);
        return actorId;
    }

    @Transactional
    public void saveMovieCast(UUID movieId, List<CastMemberDto> castList) {
        if (movieId == null || castList == null) return;

        jdbcTemplate.update("DELETE FROM [dbo].[MovieActors] WHERE MovieID = ?", movieId.toString());

        Set<UUID> addedActorIds = new HashSet<>();
        int order = 0;
        for (CastMemberDto dto : castList) {
            if (dto == null || dto.getName() == null || dto.getName().trim().isEmpty()) continue;
            UUID actorId = findOrCreateActor(dto.getName(), dto.getPhotoUrl(), dto.getTmdbId(), dto.getTvmazeId());
            if (actorId != null) {
                dto.setActorId(actorId);
                dto.setPhotoUrl("/media/actor/" + actorId);
                if (addedActorIds.add(actorId)) {
                    int orderIndex = dto.getOrderIndex() > 0 ? dto.getOrderIndex() : order++;
                    jdbcTemplate.update(
                            "INSERT INTO [dbo].[MovieActors] (MovieID, ActorID, CharacterName, OrderIndex) VALUES (?, ?, ?, ?)",
                            movieId.toString(), actorId.toString(), dto.getCharacterName(), orderIndex);
                }
            }
        }
    }

    @Transactional
    public void saveSeriesCast(UUID seriesId, List<CastMemberDto> castList) {
        if (seriesId == null || castList == null) return;

        jdbcTemplate.update("DELETE FROM [dbo].[SeriesActors] WHERE SeriesID = ?", seriesId.toString());

        Set<UUID> addedActorIds = new HashSet<>();
        int order = 0;
        for (CastMemberDto dto : castList) {
            if (dto == null || dto.getName() == null || dto.getName().trim().isEmpty()) continue;
            UUID actorId = findOrCreateActor(dto.getName(), dto.getPhotoUrl(), dto.getTmdbId(), dto.getTvmazeId());
            if (actorId != null) {
                dto.setActorId(actorId);
                dto.setPhotoUrl("/media/actor/" + actorId);
                if (addedActorIds.add(actorId)) {
                    int orderIndex = dto.getOrderIndex() > 0 ? dto.getOrderIndex() : order++;
                    jdbcTemplate.update(
                            "INSERT INTO [dbo].[SeriesActors] (SeriesID, ActorID, CharacterName, OrderIndex) VALUES (?, ?, ?, ?)",
                            seriesId.toString(), actorId.toString(), dto.getCharacterName(), orderIndex);
                }
            }
        }
    }

    public List<CastMemberDto> getCastForMovie(UUID movieId) {
        String sql = "SELECT a.ID as ActorId, a.Name, a.PhotoUrl, a.TmdbId, a.TvMazeId, ma.CharacterName, ma.OrderIndex " +
                     "FROM [dbo].[MovieActors] ma " +
                     "JOIN [dbo].[Actor] a ON ma.ActorID = a.ID " +
                     "WHERE ma.MovieID = ? " +
                     "ORDER BY ma.OrderIndex ASC";
        return jdbcTemplate.query(sql, castMemberRowMapper, movieId.toString());
    }

    public List<CastMemberDto> getCastForSeries(UUID seriesId) {
        String sql = "SELECT a.ID as ActorId, a.Name, a.PhotoUrl, a.TmdbId, a.TvMazeId, sa.CharacterName, sa.OrderIndex " +
                     "FROM [dbo].[SeriesActors] sa " +
                     "JOIN [dbo].[Actor] a ON sa.ActorID = a.ID " +
                     "WHERE sa.SeriesID = ? " +
                     "ORDER BY sa.OrderIndex ASC";
        return jdbcTemplate.query(sql, castMemberRowMapper, seriesId.toString());
    }

    public boolean hasCastForMovie(UUID movieId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM [dbo].[MovieActors] WHERE MovieID = ?", Integer.class, movieId.toString());
        return count != null && count > 0;
    }

    public boolean hasCastForSeries(UUID seriesId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM [dbo].[SeriesActors] WHERE SeriesID = ?", Integer.class, seriesId.toString());
        return count != null && count > 0;
    }
}

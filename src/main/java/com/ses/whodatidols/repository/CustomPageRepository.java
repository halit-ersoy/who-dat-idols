package com.ses.whodatidols.repository;

import com.ses.whodatidols.model.CustomPage;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class CustomPageRepository {

    private static final Logger logger = LoggerFactory.getLogger(CustomPageRepository.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        try {
            String sql = "IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[CustomPages]') AND type in (N'U')) " +
                    "CREATE TABLE [dbo].[CustomPages] (" +
                    "[ID] UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(), " +
                    "[title] NVARCHAR(255) NOT NULL, " +
                    "[slug] NVARCHAR(128) NOT NULL UNIQUE, " +
                    "[targetSite] NVARCHAR(50) NOT NULL DEFAULT 'whodatidols', " +
                    "[entryFile] NVARCHAR(255) DEFAULT 'index.html', " +
                    "[storagePath] NVARCHAR(500) NOT NULL, " +
                    "[totalSize] BIGINT DEFAULT 0, " +
                    "[fileCount] INT DEFAULT 0, " +
                    "[isActive] BIT DEFAULT 1, " +
                    "[createdAt] DATETIME DEFAULT GETUTCDATE(), " +
                    "[updatedAt] DATETIME DEFAULT GETUTCDATE()); " +
                    "IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[CustomPages]') AND name = 'targetSite') " +
                    "ALTER TABLE [dbo].[CustomPages] ADD [targetSite] NVARCHAR(50) NOT NULL DEFAULT 'whodatidols';";
            jdbcTemplate.execute(sql);
            logger.info("CustomPages table and targetSite column verified/created successfully.");
        } catch (Exception e) {
            logger.error("Error creating CustomPages table: ", e);
        }
    }

    public List<CustomPage> findAll() {
        String sql = "SELECT * FROM CustomPages ORDER BY createdAt DESC";
        return jdbcTemplate.query(sql, new CustomPageRowMapper());
    }

    public Optional<CustomPage> findById(UUID id) {
        String sql = "SELECT * FROM CustomPages WHERE ID = ?";
        try {
            CustomPage page = jdbcTemplate.queryForObject(sql, new CustomPageRowMapper(), id);
            return Optional.ofNullable(page);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<CustomPage> findBySlug(String slug) {
        if (slug == null || slug.isBlank()) return Optional.empty();
        String sql = "SELECT * FROM CustomPages WHERE LOWER(slug) = LOWER(?)";
        try {
            CustomPage page = jdbcTemplate.queryForObject(sql, new CustomPageRowMapper(), slug.trim());
            return Optional.ofNullable(page);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<CustomPage> findActiveBySlug(String slug) {
        if (slug == null || slug.isBlank()) return Optional.empty();
        String sql = "SELECT * FROM CustomPages WHERE LOWER(slug) = LOWER(?) AND isActive = 1";
        try {
            CustomPage page = jdbcTemplate.queryForObject(sql, new CustomPageRowMapper(), slug.trim());
            return Optional.ofNullable(page);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public boolean existsBySlug(String slug) {
        if (slug == null || slug.isBlank()) return false;
        String sql = "SELECT COUNT(*) FROM CustomPages WHERE LOWER(slug) = LOWER(?)";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, slug.trim());
        return count != null && count > 0;
    }

    public void save(CustomPage page) {
        if (page.getId() == null) {
            page.setId(UUID.randomUUID());
        }
        String sql = "INSERT INTO CustomPages (ID, title, slug, targetSite, entryFile, storagePath, totalSize, fileCount, isActive, createdAt, updatedAt) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                page.getId(),
                page.getTitle(),
                page.getSlug().toLowerCase().trim(),
                page.getTargetSite(),
                page.getEntryFile(),
                page.getStoragePath(),
                page.getTotalSize(),
                page.getFileCount(),
                page.isActive(),
                Timestamp.from(page.getCreatedAt() != null ? page.getCreatedAt() : Instant.now()),
                Timestamp.from(page.getUpdatedAt() != null ? page.getUpdatedAt() : Instant.now())
        );
    }

    public void update(CustomPage page) {
        String sql = "UPDATE CustomPages SET title = ?, slug = ?, targetSite = ?, entryFile = ?, storagePath = ?, " +
                "totalSize = ?, fileCount = ?, isActive = ?, updatedAt = ? WHERE ID = ?";
        jdbcTemplate.update(sql,
                page.getTitle(),
                page.getSlug().toLowerCase().trim(),
                page.getTargetSite(),
                page.getEntryFile(),
                page.getStoragePath(),
                page.getTotalSize(),
                page.getFileCount(),
                page.isActive(),
                Timestamp.from(Instant.now()),
                page.getId()
        );
    }

    public boolean toggleActive(UUID id) {
        String sql = "UPDATE CustomPages SET isActive = CASE WHEN isActive = 1 THEN 0 ELSE 1 END, updatedAt = ? WHERE ID = ?";
        int updated = jdbcTemplate.update(sql, Timestamp.from(Instant.now()), id);
        return updated > 0;
    }

    public boolean deleteById(UUID id) {
        String sql = "DELETE FROM CustomPages WHERE ID = ?";
        int deleted = jdbcTemplate.update(sql, id);
        return deleted > 0;
    }

    private static class CustomPageRowMapper implements RowMapper<CustomPage> {
        @Override
        public CustomPage mapRow(ResultSet rs, int rowNum) throws SQLException {
            Object rawId = rs.getObject("ID");
            UUID id = null;
            if (rawId instanceof UUID) {
                id = (UUID) rawId;
            } else if (rawId != null) {
                try {
                    id = UUID.fromString(rawId.toString());
                } catch (Exception ignored) {
                }
            }
            String title = rs.getString("title");
            String slug = rs.getString("slug");
            String targetSite = null;
            try {
                targetSite = rs.getString("targetSite");
            } catch (Exception ignored) {}
            if (targetSite == null || targetSite.isBlank()) targetSite = "whodatidols";

            String entryFile = rs.getString("entryFile");
            String storagePath = rs.getString("storagePath");
            long totalSize = rs.getLong("totalSize");
            int fileCount = rs.getInt("fileCount");
            boolean isActive = rs.getBoolean("isActive");
            Timestamp createdAt = rs.getTimestamp("createdAt");
            Timestamp updatedAt = rs.getTimestamp("updatedAt");

            return new CustomPage(
                    id,
                    title,
                    slug,
                    entryFile,
                    storagePath,
                    totalSize,
                    fileCount,
                    isActive,
                    createdAt != null ? createdAt.toInstant() : Instant.now(),
                    updatedAt != null ? updatedAt.toInstant() : Instant.now(),
                    targetSite
            );
        }
    }
}

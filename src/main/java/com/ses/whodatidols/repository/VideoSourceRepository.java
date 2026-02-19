package com.ses.whodatidols.repository;

import com.ses.whodatidols.model.VideoSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class VideoSourceRepository {
    private static final Logger logger = LoggerFactory.getLogger(VideoSourceRepository.class);

    private final JdbcTemplate jdbcTemplate;

    public VideoSourceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<VideoSource> rowMapper = (rs, rowNum) -> {
        VideoSource source = new VideoSource();
        String idStr = rs.getString("ID");
        source.setId(idStr != null ? UUID.fromString(idStr) : null);

        String contentIdStr = rs.getString("ContentId");
        source.setContentId(contentIdStr != null ? UUID.fromString(contentIdStr) : null);

        source.setSourceName(rs.getString("SourceName") != null ? rs.getString("SourceName") : "Bilinmeyen Kaynak");
        source.setSourceUrl(rs.getString("SourceUrl") != null ? rs.getString("SourceUrl") : "");
        source.setSortOrder(rs.getInt("SortOrder"));
        return source;
    };

    public List<VideoSource> findByContentId(UUID contentId) {
        String sql = "EXEC dbo.GetVideoSources ?";
        logger.info("Fetching sources for contentId: {}", contentId);
        @SuppressWarnings("null")
        List<VideoSource> results = jdbcTemplate.query(sql, rowMapper, contentId.toString());
        return results;
    }

    public void save(VideoSource source) {
        if (source.getId() == null) {
            source.setId(UUID.randomUUID());
        }
        logger.info("Saving VideoSource: id={}, contentId={}, name={}",
                source.getId(), source.getContentId(), source.getSourceName());

        String sql = "INSERT INTO VideoSource (ID, ContentId, SourceName, SourceUrl, SortOrder) VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                source.getId().toString(),
                source.getContentId().toString(),
                source.getSourceName(),
                source.getSourceUrl(),
                source.getSortOrder());
    }

    public void delete(UUID id) {
        String sql = "EXEC dbo.DeleteVideoSource ?";
        jdbcTemplate.update(sql, id.toString());
    }

    public void deleteAllForContent(UUID contentId) {
        String sql = "EXEC dbo.DeleteAllVideoSourcesForContent ?";
        logger.info("Deleting all sources for contentId: {}", contentId);
        jdbcTemplate.update(sql, contentId.toString());
    }
}

package com.ses.whodatidols.repository;

import com.ses.whodatidols.model.Ad;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class AdRepository {
    private static final Logger logger = LoggerFactory.getLogger(AdRepository.class);
    private final JdbcTemplate jdbcTemplate;

    public AdRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        ensureSchema();
    }

    private void ensureSchema() {
        try {
            jdbcTemplate.execute(
                    "IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'Ad') " +
                    "BEGIN " +
                    "    CREATE TABLE Ad ( " +
                    "        ID NVARCHAR(36) PRIMARY KEY, " +
                    "        Name NVARCHAR(255) NOT NULL, " +
                    "        UploadDate DATETIME NOT NULL " +
                    "    ); " +
                    "END"
            );
            // Migration: Add IsHidden column if it doesn't exist
            jdbcTemplate.execute(
                    "IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('Ad') AND name = 'IsHidden') " +
                    "BEGIN " +
                    "    ALTER TABLE Ad ADD IsHidden BIT NOT NULL DEFAULT 0; " +
                    "END"
            );
            logger.info("Ad table schema verified successfully.");
        } catch (Exception e) {
            logger.error("Ad schema update failed: {}", e.getMessage());
        }
    }

    private final @NonNull RowMapper<Ad> rowMapper = (rs, rowNum) -> {
        Ad ad = new Ad();
        String idStr = rs.getString("ID");
        ad.setId(idStr != null ? UUID.fromString(idStr) : null);
        ad.setName(rs.getString("Name"));
        java.sql.Timestamp ts = rs.getTimestamp("UploadDate");
        if (ts != null) {
            ad.setUploadDate(ts.toInstant());
        }
        ad.setHidden(rs.getBoolean("IsHidden"));
        return ad;
    };

    public List<Ad> findAll() {
        String sql = "SELECT ID, Name, UploadDate, IsHidden FROM Ad ORDER BY UploadDate DESC";
        return jdbcTemplate.query(sql, rowMapper);
    }

    public List<Ad> findVisible() {
        String sql = "SELECT ID, Name, UploadDate, IsHidden FROM Ad WHERE IsHidden = 0 ORDER BY UploadDate DESC";
        return jdbcTemplate.query(sql, rowMapper);
    }

    public Ad findById(UUID id) {
        String sql = "SELECT ID, Name, UploadDate, IsHidden FROM Ad WHERE ID = ?";
        try {
            return jdbcTemplate.queryForObject(sql, rowMapper, id.toString());
        } catch (Exception e) {
            return null;
        }
    }

    public void save(Ad ad) {
        if (ad.getId() == null) {
            ad.setId(UUID.randomUUID());
        }
        String sql = "INSERT INTO Ad (ID, Name, UploadDate, IsHidden) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                ad.getId().toString(),
                ad.getName(),
                ad.getUploadDate() != null ? java.sql.Timestamp.from(ad.getUploadDate()) : java.sql.Timestamp.from(java.time.Instant.now()),
                ad.isHidden() ? 1 : 0
        );
    }

    public void updateHidden(UUID id, boolean isHidden) {
        String sql = "UPDATE Ad SET IsHidden = ? WHERE ID = ?";
        jdbcTemplate.update(sql, isHidden ? 1 : 0, id.toString());
    }

    public void deleteById(UUID id) {
        String sql = "DELETE FROM Ad WHERE ID = ?";
        jdbcTemplate.update(sql, id.toString());
    }
}

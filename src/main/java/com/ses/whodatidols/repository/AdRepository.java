package com.ses.whodatidols.repository;

import com.ses.whodatidols.model.Ad;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
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
            logger.info("Ad table schema verified successfully.");
        } catch (Exception e) {
            logger.error("Ad schema update failed: {}", e.getMessage());
        }
    }

    private final RowMapper<Ad> rowMapper = (rs, rowNum) -> {
        Ad ad = new Ad();
        String idStr = rs.getString("ID");
        ad.setId(idStr != null ? UUID.fromString(idStr) : null);
        ad.setName(rs.getString("Name"));
        java.sql.Timestamp ts = rs.getTimestamp("UploadDate");
        if (ts != null) {
            ad.setUploadDate(ts.toInstant());
        }
        return ad;
    };

    public List<Ad> findAll() {
        String sql = "SELECT ID, Name, UploadDate FROM Ad ORDER BY UploadDate DESC";
        return jdbcTemplate.query(sql, rowMapper);
    }

    public Ad findById(UUID id) {
        String sql = "SELECT ID, Name, UploadDate FROM Ad WHERE ID = ?";
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
        String sql = "INSERT INTO Ad (ID, Name, UploadDate) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql,
                ad.getId().toString(),
                ad.getName(),
                ad.getUploadDate() != null ? java.sql.Timestamp.from(ad.getUploadDate()) : java.sql.Timestamp.from(java.time.Instant.now())
        );
    }

    public void deleteById(UUID id) {
        String sql = "DELETE FROM Ad WHERE ID = ?";
        jdbcTemplate.update(sql, id.toString());
    }
}

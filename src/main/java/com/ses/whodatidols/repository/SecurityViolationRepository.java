package com.ses.whodatidols.repository;

import com.ses.whodatidols.model.SecurityViolation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class SecurityViolationRepository {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public SecurityViolationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        String sql = """
                IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[WhoDatIdols].[dbo].[SecurityViolations]') AND type in (N'U'))
                BEGIN
                    CREATE TABLE [WhoDatIdols].[dbo].[SecurityViolations] (
                        ID BIGINT IDENTITY(1,1) PRIMARY KEY,
                        IpAddress NVARCHAR(255),
                        UserAgent NVARCHAR(MAX),
                        PageUrl NVARCHAR(MAX),
                        Timestamp DATETIME DEFAULT GETDATE()
                    )
                END
                """;
        jdbcTemplate.execute(sql);
    }

    public void save(String ipAddress, String userAgent, String pageUrl) {
        String sql = "INSERT INTO [WhoDatIdols].[dbo].[SecurityViolations] (IpAddress, UserAgent, PageUrl) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, ipAddress, userAgent, pageUrl);
    }

    public List<SecurityViolation> findAllByOrderByTimestampDesc() {
        String sql = "SELECT * FROM [WhoDatIdols].[dbo].[SecurityViolations] ORDER BY Timestamp DESC";
        return jdbcTemplate.query(sql, (rs, rowNum) -> new SecurityViolation(
                rs.getLong("ID"),
                rs.getString("IpAddress"),
                rs.getString("UserAgent"),
                rs.getString("PageUrl"),
                rs.getTimestamp("Timestamp").toLocalDateTime()));
    }

    public void deleteById(Long id) {
        String sql = "DELETE FROM [WhoDatIdols].[dbo].[SecurityViolations] WHERE ID = ?";
        jdbcTemplate.update(sql, id);
    }

    public void deleteByIp(String ipAddress) {
        String sql = "DELETE FROM [WhoDatIdols].[dbo].[SecurityViolations] WHERE IpAddress = ?";
        jdbcTemplate.update(sql, ipAddress);
    }

    public void deleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty())
            return;
        String placeholders = ids.stream().map(id -> "?").collect(java.util.stream.Collectors.joining(","));
        String sql = "DELETE FROM [WhoDatIdols].[dbo].[SecurityViolations] WHERE ID IN (" + placeholders + ")";
        jdbcTemplate.update(sql, ids.toArray());
    }
}

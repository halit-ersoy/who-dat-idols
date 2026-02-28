package com.ses.whodatidols.repository;

import com.ses.whodatidols.model.BannedIp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class BannedIpRepository {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public BannedIpRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        createTableAndMigrate();
    }

    private void createTableAndMigrate() {
        String createTableSql = """
                IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[WhoDatIdols].[dbo].[BannedIps]') AND type in (N'U'))
                BEGIN
                    CREATE TABLE [WhoDatIdols].[dbo].[BannedIps] (
                        ID BIGINT IDENTITY(1,1) PRIMARY KEY,
                        IpAddress NVARCHAR(255) UNIQUE,
                        Reason NVARCHAR(MAX),
                        AppealMessage NVARCHAR(100),
                        Timestamp DATETIME DEFAULT GETUTCDATE()
                    )
                END
                ELSE
                BEGIN
                    IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID(N'[WhoDatIdols].[dbo].[BannedIps]') AND name = 'AppealMessage')
                    BEGIN
                        ALTER TABLE [WhoDatIdols].[dbo].[BannedIps] ADD AppealMessage NVARCHAR(100)
                    END
                END
                """;
        jdbcTemplate.execute(createTableSql);
    }

    public void save(String ipAddress, String reason) {
        String sql = "IF NOT EXISTS (SELECT 1 FROM [WhoDatIdols].[dbo].[BannedIps] WHERE IpAddress = ?) " +
                "INSERT INTO [WhoDatIdols].[dbo].[BannedIps] (IpAddress, Reason, Timestamp) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, ipAddress, ipAddress, reason, java.sql.Timestamp.from(Instant.now()));
    }

    public void updateAppeal(String ipAddress, String appealMessage) {
        String sql = "UPDATE [WhoDatIdols].[dbo].[BannedIps] SET AppealMessage = ? WHERE IpAddress = ? AND (AppealMessage IS NULL OR AppealMessage = '')";
        jdbcTemplate.update(sql, appealMessage, ipAddress);
    }

    public boolean isBanned(String ipAddress) {
        String sql = "SELECT COUNT(*) FROM [WhoDatIdols].[dbo].[BannedIps] WHERE IpAddress = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, ipAddress);
        return count != null && count > 0;
    }

    public Optional<BannedIp> findByIp(String ipAddress) {
        String sql = "SELECT * FROM [WhoDatIdols].[dbo].[BannedIps] WHERE IpAddress = ?";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            java.sql.Timestamp ts = rs.getTimestamp("Timestamp");
            return new BannedIp(
                    rs.getLong("ID"),
                    rs.getString("IpAddress"),
                    rs.getString("Reason"),
                    rs.getString("AppealMessage"),
                    ts != null ? ts.toInstant() : null);
        }, ipAddress).stream().findFirst();
    }

    public List<BannedIp> findAll() {
        String sql = "SELECT * FROM [WhoDatIdols].[dbo].[BannedIps] ORDER BY Timestamp DESC";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            java.sql.Timestamp ts = rs.getTimestamp("Timestamp");
            return new BannedIp(
                    rs.getLong("ID"),
                    rs.getString("IpAddress"),
                    rs.getString("Reason"),
                    rs.getString("AppealMessage"),
                    ts != null ? ts.toInstant() : null);
        });
    }

    public void deleteByIp(String ipAddress) {
        String sql = "DELETE FROM [WhoDatIdols].[dbo].[BannedIps] WHERE IpAddress = ?";
        jdbcTemplate.update(sql, ipAddress);
    }

    public void deleteById(Long id) {
        String sql = "DELETE FROM [WhoDatIdols].[dbo].[BannedIps] WHERE ID = ?";
        jdbcTemplate.update(sql, id);
    }
}

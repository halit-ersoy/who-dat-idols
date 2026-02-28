package com.ses.whodatidols.repository;

import com.ses.whodatidols.model.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class MessageRepository {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public MessageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private void ensureMessageTableExists() {
        String sql = "IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'UserMessages') " +
                "CREATE TABLE UserMessages (" +
                "ID UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID()," +
                "SenderID UNIQUEIDENTIFIER NOT NULL," +
                "ReceiverID UNIQUEIDENTIFIER NOT NULL," +
                "Content NVARCHAR(MAX) NOT NULL," +
                "Timestamp DATETIME DEFAULT GETUTCDATE()," +
                "IsRead BIT DEFAULT 0," +
                "FOREIGN KEY (SenderID) REFERENCES Person(ID)," +
                "FOREIGN KEY (ReceiverID) REFERENCES Person(ID)" +
                ")";
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception e) {
            System.err.println("Error creating UserMessages table: " + e.getMessage());
        }
    }

    private void ensureUserBlocksTableExists() {
        String sql = "IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'UserBlocks') " +
                "CREATE TABLE UserBlocks (" +
                "BlockerID UNIQUEIDENTIFIER NOT NULL," +
                "BlockedID UNIQUEIDENTIFIER NOT NULL," +
                "CreatedAt DATETIME DEFAULT GETUTCDATE()," +
                "PRIMARY KEY (BlockerID, BlockedID)," +
                "FOREIGN KEY (BlockerID) REFERENCES Person(ID)," +
                "FOREIGN KEY (BlockedID) REFERENCES Person(ID)" +
                ")";
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception e) {
            System.err.println("Error creating UserBlocks table: " + e.getMessage());
        }
    }

    private void ensureUserReportsTableExists() {
        String sql = "IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'UserReports') " +
                "CREATE TABLE UserReports (" +
                "ID UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID()," +
                "ReporterID UNIQUEIDENTIFIER NOT NULL," +
                "ReportedID UNIQUEIDENTIFIER NOT NULL," +
                "Reason NVARCHAR(255) NOT NULL," +
                "ContentContext NVARCHAR(MAX)," +
                "CreatedAt DATETIME DEFAULT GETUTCDATE()," +
                "IsResolved BIT DEFAULT 0," +
                "FOREIGN KEY (ReporterID) REFERENCES Person(ID)," +
                "FOREIGN KEY (ReportedID) REFERENCES Person(ID)" +
                ")";
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception e) {
            System.err.println("Error creating UserReports table: " + e.getMessage());
        }
    }

    public void sendMessage(UUID senderId, UUID receiverId, String content) {
        ensureMessageTableExists();
        String sql = "INSERT INTO UserMessages (SenderID, ReceiverID, Content, Timestamp) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(sql, senderId, receiverId, content, java.sql.Timestamp.from(Instant.now()));
    }

    public List<Message> getChatHistory(UUID user1, UUID user2) {
        ensureMessageTableExists();
        String sql = "SELECT m.ID, m.SenderID, m.ReceiverID, m.Content, m.Timestamp, m.IsRead, " +
                "p1.nickname as SenderNickname, p2.nickname as ReceiverNickname, p1.role as SenderRole, p2.role as ReceiverRole "
                +
                "FROM UserMessages m " +
                "JOIN Person p1 ON m.SenderID = p1.ID " +
                "JOIN Person p2 ON m.ReceiverID = p2.ID " +
                "WHERE (m.SenderID = ? AND m.ReceiverID = ?) OR (m.SenderID = ? AND m.ReceiverID = ?) " +
                "ORDER BY m.Timestamp ASC";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Message m = new Message();
            m.setId(UUID.fromString(rs.getString("ID")));
            m.setSenderId(UUID.fromString(rs.getString("SenderID")));
            m.setReceiverId(UUID.fromString(rs.getString("ReceiverID")));
            m.setContent(rs.getString("Content"));
            java.sql.Timestamp ts = rs.getTimestamp("Timestamp");
            if (ts != null)
                m.setTimestamp(ts.toInstant());
            m.setRead(rs.getBoolean("IsRead"));
            m.setSenderNickname(rs.getString("SenderNickname"));
            m.setReceiverNickname(rs.getString("ReceiverNickname"));
            m.setSenderRole(rs.getString("SenderRole"));
            m.setReceiverRole(rs.getString("ReceiverRole"));
            return m;
        }, user1, user2, user2, user1);
    }

    public List<Message> getConversationList(UUID userId) {
        ensureMessageTableExists();
        String sqlServerPart = "SELECT ID, SenderID, ReceiverID, Content, Timestamp, IsRead, SenderNickname, ReceiverNickname, SenderRole, ReceiverRole FROM ("
                +
                "  SELECT m.*, p1.nickname as SenderNickname, p2.nickname as ReceiverNickname, p1.role as SenderRole, p2.role as ReceiverRole, "
                +
                "  ROW_NUMBER() OVER (PARTITION BY CASE WHEN SenderID < ReceiverID THEN CAST(SenderID AS VARCHAR(36)) + CAST(ReceiverID AS VARCHAR(36)) ELSE CAST(ReceiverID AS VARCHAR(36)) + CAST(SenderID AS VARCHAR(36)) END ORDER BY Timestamp DESC) as RowNum "
                +
                "  FROM UserMessages m " +
                "  JOIN Person p1 ON m.SenderID = p1.ID " +
                "  JOIN Person p2 ON m.ReceiverID = p2.ID " +
                "  WHERE m.SenderID = ? OR m.ReceiverID = ?" +
                ") x WHERE RowNum = 1 ORDER BY Timestamp DESC";

        return jdbcTemplate.query(sqlServerPart, (rs, rowNum) -> {
            Message m = new Message();
            m.setId(UUID.fromString(rs.getString("ID")));
            m.setSenderId(UUID.fromString(rs.getString("SenderID")));
            m.setReceiverId(UUID.fromString(rs.getString("ReceiverID")));
            m.setContent(rs.getString("Content"));
            java.sql.Timestamp ts = rs.getTimestamp("Timestamp");
            if (ts != null)
                m.setTimestamp(ts.toInstant());
            m.setRead(rs.getBoolean("IsRead"));
            m.setSenderNickname(rs.getString("SenderNickname"));
            m.setReceiverNickname(rs.getString("ReceiverNickname"));
            m.setSenderRole(rs.getString("SenderRole"));
            m.setReceiverRole(rs.getString("ReceiverRole"));
            return m;
        }, userId, userId);
    }

    public void markAsRead(UUID senderId, UUID receiverId) {
        ensureMessageTableExists();
        String sql = "UPDATE UserMessages SET IsRead = 1 WHERE SenderID = ? AND ReceiverID = ? AND IsRead = 0";
        jdbcTemplate.update(sql, senderId, receiverId);
    }

    public List<Message> getAllConversations() {
        ensureMessageTableExists();
        String sql = "SELECT ID, SenderID, ReceiverID, Content, Timestamp, IsRead, SenderNickname, ReceiverNickname, SenderRole, ReceiverRole FROM ("
                +
                "  SELECT m.*, p1.nickname as SenderNickname, p2.nickname as ReceiverNickname, p1.role as SenderRole, p2.role as ReceiverRole, "
                +
                "  ROW_NUMBER() OVER (PARTITION BY CASE WHEN SenderID < ReceiverID THEN CAST(SenderID AS VARCHAR(36)) + CAST(ReceiverID AS VARCHAR(36)) ELSE CAST(ReceiverID AS VARCHAR(36)) + CAST(SenderID AS VARCHAR(36)) END ORDER BY Timestamp DESC) as RowNum "
                +
                "  FROM UserMessages m " +
                "  JOIN Person p1 ON m.SenderID = p1.ID " +
                "  JOIN Person p2 ON m.ReceiverID = p2.ID "
                + ") x WHERE RowNum = 1 ORDER BY Timestamp DESC";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Message m = new Message();
            m.setId(UUID.fromString(rs.getString("ID")));
            m.setSenderId(UUID.fromString(rs.getString("SenderID")));
            m.setReceiverId(UUID.fromString(rs.getString("ReceiverID")));
            m.setContent(rs.getString("Content"));
            java.sql.Timestamp ts = rs.getTimestamp("Timestamp");
            if (ts != null)
                m.setTimestamp(ts.toInstant());
            m.setRead(rs.getBoolean("IsRead"));
            m.setSenderNickname(rs.getString("SenderNickname"));
            m.setReceiverNickname(rs.getString("ReceiverNickname"));
            m.setSenderRole(rs.getString("SenderRole"));
            m.setReceiverRole(rs.getString("ReceiverRole"));
            return m;
        });
    }

    public boolean isBlocked(UUID u1, UUID u2) {
        ensureUserBlocksTableExists();
        String sql = "SELECT COUNT(*) FROM UserBlocks WHERE (BlockerID = ? AND BlockedID = ?) OR (BlockerID = ? AND BlockedID = ?)";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, u1, u2, u2, u1);
        return count != null && count > 0;
    }

    public boolean isBlockedByMe(UUID me, UUID them) {
        ensureUserBlocksTableExists();
        String sql = "SELECT COUNT(*) FROM UserBlocks WHERE BlockerID = ? AND BlockedID = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, me, them);
        return count != null && count > 0;
    }

    public void blockUser(UUID blockerId, UUID blockedId) {
        ensureUserBlocksTableExists();
        String sql = "IF NOT EXISTS (SELECT * FROM UserBlocks WHERE BlockerID = ? AND BlockedID = ?) " +
                "INSERT INTO UserBlocks (BlockerID, BlockedID, CreatedAt) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, blockerId, blockedId, blockerId, blockedId, java.sql.Timestamp.from(Instant.now()));
    }

    public void unblockUser(UUID blockerId, UUID blockedId) {
        ensureUserBlocksTableExists();
        String sql = "DELETE FROM UserBlocks WHERE BlockerID = ? AND BlockedID = ?";
        jdbcTemplate.update(sql, blockerId, blockedId);
    }

    public void reportUser(UUID reporterId, UUID reportedId, String reason, String context) {
        ensureUserReportsTableExists();
        String sql = "INSERT INTO UserReports (ReporterID, ReportedID, Reason, ContentContext, CreatedAt) VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, reporterId, reportedId, reason, context, java.sql.Timestamp.from(Instant.now()));
    }

    public List<java.util.Map<String, Object>> getAllReports() {
        ensureUserReportsTableExists();
        String sql = "SELECT r.*, p1.nickname as ReporterNickname, p2.nickname as ReportedNickname " +
                "FROM UserReports r " +
                "JOIN Person p1 ON r.ReporterID = p1.ID " +
                "JOIN Person p2 ON r.ReportedID = p2.ID " +
                "WHERE r.IsResolved = 0 " +
                "ORDER BY r.CreatedAt DESC";
        return jdbcTemplate.queryForList(sql);
    }

    public void resolveReport(UUID reportId) {
        ensureUserReportsTableExists();
        jdbcTemplate.update("UPDATE UserReports SET IsResolved = 1 WHERE ID = ?", reportId);
    }
}

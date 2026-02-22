package com.ses.whodatidols.repository;

import com.ses.whodatidols.model.UpdateNote;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import org.springframework.lang.NonNull;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@Repository
public class UpdateNoteRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        String sql = "IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[UpdateNotes]') AND type in (N'U')) "
                +
                "CREATE TABLE [dbo].[UpdateNotes] (" +
                "[ID] UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(), " +
                "[title] NVARCHAR(255) NOT NULL, " +
                "[message] NVARCHAR(MAX) NOT NULL, " +
                "[createdAt] DATETIME DEFAULT GETDATE(), " +
                "[isActive] BIT DEFAULT 1)";
        jdbcTemplate.execute(sql);
    }

    public List<UpdateNote> findAll() {
        String sql = "SELECT * FROM UpdateNotes ORDER BY createdAt DESC";
        return jdbcTemplate.query(sql, new UpdateNoteRowMapper());
    }

    public List<UpdateNote> findActive() {
        String sql = "SELECT * FROM UpdateNotes WHERE isActive = 1 ORDER BY createdAt DESC";
        return jdbcTemplate.query(sql, new UpdateNoteRowMapper());
    }

    public void save(UpdateNote note) {
        String sql = "INSERT INTO UpdateNotes (ID, title, message, createdAt, isActive) VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                note.getId() == null ? UUID.randomUUID() : note.getId(),
                note.getTitle(),
                note.getMessage(),
                java.sql.Timestamp.valueOf(note.getCreatedAt()),
                note.isActive());
    }

    public void delete(UUID id) {
        String sql = "DELETE FROM UpdateNotes WHERE ID = ?";
        jdbcTemplate.update(sql, id);
    }

    public void toggleActive(UUID id, boolean isActive) {
        String sql = "UPDATE UpdateNotes SET isActive = ? WHERE ID = ?";
        jdbcTemplate.update(sql, isActive, id);
    }

    private static class UpdateNoteRowMapper implements RowMapper<UpdateNote> {
        @Override
        public UpdateNote mapRow(@NonNull ResultSet rs, int rowNum) throws SQLException {
            UpdateNote note = new UpdateNote();
            note.setId(UUID.fromString(rs.getString("ID")));
            note.setTitle(rs.getString("title"));
            note.setMessage(rs.getString("message"));
            note.setCreatedAt(rs.getTimestamp("createdAt").toLocalDateTime());
            note.setActive(rs.getBoolean("isActive"));
            return note;
        }
    }
}

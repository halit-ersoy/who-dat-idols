package com.ses.whodatidols.repository;

import com.ses.whodatidols.model.SoapOpera;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@Repository
public class SoapOperaRepository {
    private final JdbcTemplate jdbcTemplate;

    private static final String GET_RECENT_SOAP_OPERAS =
            "EXEC GetSoapOperasByUploadDateOffset @dayOffset = ?";

    public SoapOperaRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<SoapOpera> findRecentSoapOperas(int day) {
        return jdbcTemplate.query(GET_RECENT_SOAP_OPERAS, new SoapOperaRowMapper(), day);
    }

    public List<SoapOpera> findTop6SoapOperasByCount() {
        return jdbcTemplate.query("EXEC GetTop6SoapOperaIdsByCount", new SoapOperaRowMapper());
    }

    public String getImagePathById(UUID soapOperaId) {
        String sql = "SELECT [bannerPath] FROM [WhoDatIdols].[dbo].[SoapOpera] WHERE [ID] = ?";
        try {
            return jdbcTemplate.queryForObject(sql, String.class, soapOperaId.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private static class SoapOperaRowMapper implements RowMapper<SoapOpera> {
        @Override
        public SoapOpera mapRow(ResultSet rs, int rowNum) throws SQLException {
            SoapOpera soapOpera = new SoapOpera();
            soapOpera.setId(UUID.fromString(rs.getString("ID")));
            soapOpera.setName(rs.getString("name"));
            soapOpera.setCategory(rs.getString("category"));
            soapOpera.setContent(rs.getString("_content"));
            soapOpera.setTime(rs.getInt("time"));
            soapOpera.setLanguage(rs.getString("language"));
            soapOpera.setYear(rs.getInt("year"));
            soapOpera.setUploadDate(rs.getTimestamp("uploadDate").toLocalDateTime());
            return soapOpera;
        }
    }
}
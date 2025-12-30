package com.ses.whodatidols.repository;

import com.ses.whodatidols.model.SoapOpera;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@Repository
public class SoapOperaRepository {
    private final JdbcTemplate jdbcTemplate;

    // --- ESKİ SABİTLER (Frontend İçin) ---
    private static final String GET_RECENT_SOAP_OPERAS =
            "EXEC GetSoapOperasByUploadDateOffset @dayOffset = ?";

    public SoapOperaRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ==========================================================
    //       BÖLÜM 1: FRONTEND / API METODLARI (ESKİLER)
    // ==========================================================

    public List<SoapOpera> findRecentSoapOperas(int day) {
        // Eski RowMapper'ı kullanıyoruz (Stored Procedure uyumlu)
        return jdbcTemplate.query(GET_RECENT_SOAP_OPERAS, new SoapOperaRowMapper(), day);
    }

    public List<SoapOpera> findTop6SoapOperasByCount() {
        return jdbcTemplate.query("EXEC GetTop6SoapOperaIdsByCount", new SoapOperaRowMapper());
    }

    public String getImagePathById(UUID soapOperaId) {
        // Banner veya resim yolu
        String sql = "SELECT [_content] FROM [WhoDatIdols].[dbo].[SoapOperas] WHERE [ID] = ?";
        try {
            return jdbcTemplate.queryForObject(sql, String.class, soapOperaId.toString());
        } catch (Exception e) {
            return null;
        }
    }

    // ==========================================================
    //       BÖLÜM 2: ADMIN PANELİ METODLARI (YENİLER)
    // ==========================================================

    // --- DİZİ (ANA KAYIT) VAR MI KONTROL ET ---
    public SoapOpera findSeriesByName(String name) {
        String sql = "SELECT * FROM [WhoDatIdols].[dbo].[SoapOperas] WHERE name = ?";
        try {
            return jdbcTemplate.queryForObject(sql, new SeriesRowMapper(), name);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    // --- YENİ DİZİ OLUŞTUR (PARENT) ---
    public void createSeries(SoapOpera s) {
        String sql = "INSERT INTO [WhoDatIdols].[dbo].[SoapOperas] (ID, name, category, _content, country, final, soapOperaSeries) VALUES (?, ?, ?, ?, ?, ?, ?)";
        String initialXML = "<Seasons></Seasons>";
        jdbcTemplate.update(sql, s.getId().toString(), s.getName(), s.getCategory(), s.getContent(), s.getLanguage(), 0, initialXML);
    }

    // --- DİZİ XML GÜNCELLE (PARENT) ---
    public void updateSeriesXML(UUID seriesId, String newXml) {
        String sql = "UPDATE [WhoDatIdols].[dbo].[SoapOperas] SET soapOperaSeries = ? WHERE ID = ?";
        jdbcTemplate.update(sql, newXml, seriesId.toString());
    }

    // --- DİZİ METADATA GÜNCELLE (Listeden Edit İçin) ---
    public void updateSeriesMetadata(SoapOpera s) {
        String sql = "UPDATE [WhoDatIdols].[dbo].[SoapOperas] SET name=?, category=?, _content=?, country=? WHERE ID=?";
        jdbcTemplate.update(sql, s.getName(), s.getCategory(), s.getContent(), s.getLanguage(), s.getId().toString());
    }

    // --- BÖLÜM KAYDET (CHILD) ---
    @Transactional
    public void saveEpisode(SoapOpera s) {
        String sql = "INSERT INTO [WhoDatIdols].[dbo].[SoapOpera] (ID, time, year, uploadDate) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(sql, s.getId().toString(), s.getTime(), s.getYear(), java.sql.Timestamp.valueOf(s.getUploadDate()));
    }

    // --- TÜM DİZİLERİ LİSTELE (PARENT TABLOSU - Admin Listesi) ---
    public List<SoapOpera> findAllSeries() {
        String sql = "SELECT * FROM [WhoDatIdols].[dbo].[SoapOperas] ORDER BY name ASC";
        return jdbcTemplate.query(sql, new SeriesRowMapper());
    }

    // ==========================================================
    //           ROW MAPPERS (Sütun Eşleştiriciler)
    // ==========================================================

    // 1. ESKİ MAPPER (Stored Procedure'ler sadece temel alanları döndürebilir)
    private static class SoapOperaRowMapper implements RowMapper<SoapOpera> {
        @Override
        public SoapOpera mapRow(ResultSet rs, int rowNum) throws SQLException {
            SoapOpera soapOpera = new SoapOpera();
            soapOpera.setId(UUID.fromString(rs.getString("ID")));
            soapOpera.setName(rs.getString("name"));
            // Bazı SP'lerde category olmayabilir, try-catch ile koruyabiliriz veya direkt alırız
            try { soapOpera.setCategory(rs.getString("category")); } catch (SQLException e) {}

            // Eğer resim yolu vs lazımsa
            try { soapOpera.setContent(rs.getString("_content")); } catch (SQLException e) {}

            return soapOpera;
        }
    }

    // 2. YENİ MAPPER (Admin Paneli 'SELECT *' yaptığı için tüm detayları alır)
    private static class SeriesRowMapper implements RowMapper<SoapOpera> {
        @Override
        public SoapOpera mapRow(ResultSet rs, int rowNum) throws SQLException {
            SoapOpera s = new SoapOpera();
            s.setId(UUID.fromString(rs.getString("ID")));
            s.setName(rs.getString("name"));
            s.setCategory(rs.getString("category"));
            s.setContent(rs.getString("_content"));
            s.setLanguage(rs.getString("country"));
            s.setXmlData(rs.getString("soapOperaSeries"));
            return s;
        }
    }
}
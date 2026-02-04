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
    private static final String GET_RECENT_SOAP_OPERAS = "EXEC GetSoapOperasByUploadDateOffset @dayOffset = ?";

    public SoapOperaRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ==========================================================
    // BÖLÜM 1: FRONTEND / API METODLARI (ESKİLER)
    // ==========================================================

    public List<SoapOpera> findRecentSoapOperas(int day) {
        return jdbcTemplate.query(GET_RECENT_SOAP_OPERAS, new SoapOperaRowMapper(), day);
    }

    public List<SoapOpera> findTop6SoapOperasByCount() {
        return jdbcTemplate.query("EXEC GetTop6SoapOperaIdsByCount", new SoapOperaRowMapper());
    }

    public String getImagePathById(UUID soapOperaId) {
        String sql = "SELECT [_content] FROM [WhoDatIdols].[dbo].[SoapOperas] WHERE [ID] = ?";
        try {
            return jdbcTemplate.queryForObject(sql, String.class, soapOperaId.toString());
        } catch (Exception e) {
            return null;
        }
    }

    // ==========================================================
    // BÖLÜM 2: ADMIN PANELİ METODLARI (YENİLER & GÜNCELLENENLER)
    // ==========================================================

    // --- DİZİ (ANA KAYIT) VAR MI KONTROL ET ---
    public SoapOpera findSeriesByName(String name) {
        String sql = "SELECT * FROM [WhoDatIdols].[dbo].[SoapOperas] WHERE name = ?";
        List<SoapOpera> results = jdbcTemplate.query(sql, new SeriesRowMapper(), name);
        if (results.isEmpty()) {
            return null;
        }
        return results.get(0);
    }

    public SoapOpera findSeriesById(UUID id) {
        String sql = "SELECT * FROM [WhoDatIdols].[dbo].[SoapOperas] WHERE ID = ?";
        List<SoapOpera> results = jdbcTemplate.query(sql, new SeriesRowMapper(), id.toString());
        if (results.isEmpty()) {
            return null;
        }
        return results.get(0);
    }

    // --- YENİ DİZİ OLUŞTUR (PARENT) ---
    public void createSeries(SoapOpera s) {
        String sql = "INSERT INTO [WhoDatIdols].[dbo].[SoapOperas] (ID, name, category, _content, country, final, soapOperaSeries) VALUES (?, ?, ?, ?, ?, ?, ?)";
        String initialXML = "<Seasons></Seasons>";
        jdbcTemplate.update(sql, s.getId().toString(), s.getName(), s.getCategory(), s.getContent(), s.getLanguage(), 0,
                initialXML);
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
        String sql = "INSERT INTO [WhoDatIdols].[dbo].[SoapOpera] (ID, name, time, year, uploadDate) VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, s.getId().toString(), s.getName(), s.getTime(), s.getYear(),
                java.sql.Timestamp.valueOf(s.getUploadDate()));
    }

    // --- BÖLÜM GÜNCELLE (CHILD) ---
    public void updateEpisode(SoapOpera s) {
        String sql = "UPDATE [WhoDatIdols].[dbo].[SoapOpera] SET time = ?, year = ? WHERE ID = ?";
        jdbcTemplate.update(sql, s.getTime(), s.getYear(), s.getId().toString());
    }

    // --- TÜM DİZİLERİ LİSTELE (PARENT TABLOSU - Admin Listesi) ---
    public List<SoapOpera> findAllSeries() {
        String sql = "SELECT * FROM [WhoDatIdols].[dbo].[SoapOperas] ORDER BY name ASC";
        return jdbcTemplate.query(sql, new SeriesRowMapper());
    }

    // ----------------------------------------------------------
    // BÖLÜM 3: SİLME OPERASYONLARI (DELETE) - EKLENDİ
    // ----------------------------------------------------------

    // 1. Diziyi İsme Göre Sil (Parent)
    public void deleteSeriesByName(String name) {
        String sql = "DELETE FROM [WhoDatIdols].[dbo].[SoapOperas] WHERE name = ?";
        jdbcTemplate.update(sql, name);
    }

    public void deleteSeriesById(UUID id) {
        String sql = "DELETE FROM [WhoDatIdols].[dbo].[SoapOperas] WHERE ID = ?";
        jdbcTemplate.update(sql, id.toString());
    }

    // 2. Bölümü ID'ye Göre Sil (Child)
    public void deleteEpisodeById(UUID id) {
        String sql = "DELETE FROM [WhoDatIdols].[dbo].[SoapOpera] WHERE ID = ?";
        jdbcTemplate.update(sql, id.toString());
    }

    // 3. Bölüm ID'sini içeren Diziyi Bul (XML İçinde Arama Yapar)
    // Bu, XML'den o bölümü silmek için hangi diziyi güncelleyeceğimizi bulur.
    public SoapOpera findSeriesByEpisodeIdInsideXML(String episodeUUID) {
        // SQL Server XML sütununda string araması (Basit yöntem)
        String sql = "SELECT * FROM [WhoDatIdols].[dbo].[SoapOperas] WHERE soapOperaSeries LIKE ?";
        try {
            // ID'yi çevreleyen tagleri aramayız çünkü format değişebilir, sadece UUID'yi
            // ararız
            String searchTerm = "%" + episodeUUID + "%";
            return jdbcTemplate.queryForObject(sql, new SeriesRowMapper(), searchTerm);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    // --- SON EKLENEN BÖLÜMLERİ GETİR (CHILD TABLE) ---
    public List<SoapOpera> findRecentEpisodes(int limit) {
        String sql = "SELECT TOP (?) * FROM [WhoDatIdols].[dbo].[SoapOpera] ORDER BY uploadDate DESC";
        // 'SoapOperaRowMapper' kullanıyoruz çünkü bu tablo Child (SoapOpera) tablosu
        return jdbcTemplate.query(sql, new SoapOperaRowMapper(), limit);
    }

    // ==========================================================
    // ROW MAPPERS
    // ==========================================================

    private static class SoapOperaRowMapper implements RowMapper<SoapOpera> {
        @Override
        public SoapOpera mapRow(ResultSet rs, int rowNum) throws SQLException {
            SoapOpera soapOpera = new SoapOpera();
            soapOpera.setId(UUID.fromString(rs.getString("ID")));
            soapOpera.setName(rs.getString("name"));
            try {
                soapOpera.setTime(rs.getInt("time"));
                soapOpera.setYear(rs.getInt("year"));
                if (rs.getTimestamp("uploadDate") != null) {
                    soapOpera.setUploadDate(rs.getTimestamp("uploadDate").toLocalDateTime());
                }
            } catch (SQLException e) {
                // Sütunlar yoksa (Parent tablosuyla karışırsa) yoksay
            }
            // Child tablosunda category, _content, country, ... yok.
            // Ancak SoapOperaRowMapper yukarıda tanımlıydı, ona dikkat edelim.
            return soapOpera;
        }
    }

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
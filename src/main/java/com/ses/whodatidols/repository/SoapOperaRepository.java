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

    public SoapOperaRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // --- KAYIT (INSERT) İŞLEMİ ---
    // Veriler iki tabloya dağıldığı için Transactional kullanıyoruz, biri hata verirse diğeri de geri alınsın.
    @Transactional
    public void save(SoapOpera soapOpera) {
        // 1. Tablo: Genel Bilgiler (SoapOperas)
        String sql1 = "INSERT INTO [WhoDatIdols].[dbo].[SoapOperas] " +
                "(ID, name, category, _content, country) " +
                "VALUES (?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql1,
                soapOpera.getId().toString(),
                soapOpera.getName(),
                soapOpera.getCategory(),
                soapOpera.getContent(), // DB: _content
                soapOpera.getLanguage() // DB: country sütununa yazıyoruz
        );

        // 2. Tablo: Detaylar (SoapOpera)
        String sql2 = "INSERT INTO [WhoDatIdols].[dbo].[SoapOpera] " +
                "(ID, time, year, uploadDate) " +
                "VALUES (?, ?, ?, ?)";

        jdbcTemplate.update(sql2,
                soapOpera.getId().toString(),
                soapOpera.getTime(),
                soapOpera.getYear(),
                java.sql.Timestamp.valueOf(soapOpera.getUploadDate())
        );
    }

    // --- OKUMA İŞLEMLERİ (JOIN GEREKTİRİR) ---
    // Eğer Stored Procedure kullanmıyorsanız, manuel JOIN sorgusu şöyledir:
    public List<SoapOpera> findAll() {
        String sql = "SELECT t1.ID, t1.name, t1.category, t1._content, t1.country, " +
                "t2.time, t2.year, t2.uploadDate " +
                "FROM [WhoDatIdols].[dbo].[SoapOperas] t1 " +
                "INNER JOIN [WhoDatIdols].[dbo].[SoapOpera] t2 ON t1.ID = t2.ID";

        return jdbcTemplate.query(sql, new SoapOperaRowMapper());
    }

    // Eğer eski Stored Procedure'ü kullanacaksanız (RowMapper uyumlu olmalı):
    public List<SoapOpera> findRecentSoapOperas(int day) {
        String sql = "EXEC GetSoapOperasByUploadDateOffset @dayOffset = ?";
        return jdbcTemplate.query(sql, new SoapOperaRowMapper(), day);
    }

    public List<SoapOpera> findTop6SoapOperasByCount() {
        return jdbcTemplate.query("EXEC GetTop6SoapOperaIdsByCount", new SoapOperaRowMapper());
    }

    public String getImagePathById(UUID soapOperaId) {
        // Görsel yolu genellikle _content veya ayrı bir bannerPath sütunundadır.
        // Sizin verdiğiniz SQL'de _content var, onu çekiyoruz.
        String sql = "SELECT [_content] FROM [WhoDatIdols].[dbo].[SoapOperas] WHERE [ID] = ?";
        try {
            return jdbcTemplate.queryForObject(sql, String.class, soapOperaId.toString());
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    // --- ROW MAPPER ---
    private static class SoapOperaRowMapper implements RowMapper<SoapOpera> {
        @Override
        public SoapOpera mapRow(ResultSet rs, int rowNum) throws SQLException {
            SoapOpera soapOpera = new SoapOpera();
            soapOpera.setId(UUID.fromString(rs.getString("ID")));
            soapOpera.setName(rs.getString("name"));
            soapOpera.setCategory(rs.getString("category"));

            // Sütun kontrolü yaparak alıyoruz (Bazı sorgularda hepsi dönmeyebilir)
            try { soapOpera.setContent(rs.getString("_content")); } catch (SQLException e) {}
            try { soapOpera.setLanguage(rs.getString("country")); } catch (SQLException e) {} // country -> language
            try { soapOpera.setTime(rs.getInt("time")); } catch (SQLException e) {}
            try { soapOpera.setYear(rs.getInt("year")); } catch (SQLException e) {}

            try {
                if (rs.getTimestamp("uploadDate") != null) {
                    soapOpera.setUploadDate(rs.getTimestamp("uploadDate").toLocalDateTime());
                }
            } catch (SQLException e) {}

            return soapOpera;
        }
    }
}
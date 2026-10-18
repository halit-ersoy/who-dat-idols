package com.ses.whodatidols.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@SuppressWarnings({"SqlResolve", "SqlNoDataSourceInspection"})
public class SystemSettingRepository {
    private static final Logger logger = LoggerFactory.getLogger(SystemSettingRepository.class);
    private final JdbcTemplate jdbcTemplate;
    private final Map<String, String> settingsCache = new ConcurrentHashMap<>();

    public SystemSettingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        ensureSchema();
        initDefaults();
        refreshCache();
    }

    public void refreshCache() {
        try {
            jdbcTemplate.query("SELECT SettingKey, SettingValue FROM SystemSettings", rs -> {
                settingsCache.put(rs.getString("SettingKey"),
                        rs.getString("SettingValue") != null ? rs.getString("SettingValue") : "");
            });
            logger.info("System settings cache refreshed. Total items: {}", settingsCache.size());
        } catch (Exception e) {
            logger.error("Failed to refresh system settings cache", e);
        }
    }

    private void ensureSchema() {
        try {
            // Check if table exists
            String checkSql = "SELECT count(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'SystemSettings'";
            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class);

            if (count == null || count == 0) {
                logger.info("Creating SystemSettings table...");
                jdbcTemplate.execute("""
                        CREATE TABLE SystemSettings (
                            SettingKey NVARCHAR(50) PRIMARY KEY,
                            SettingValue NVARCHAR(MAX),
                            Description NVARCHAR(255)
                        )
                        """);
            }
        } catch (Exception e) {
            logger.error("Failed to ensure SystemSettings schema", e);
        }

        try {
            // Ensure Upcoming Table schema columns exist
            jdbcTemplate.execute("""
                    IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'Upcoming' AND COLUMN_NAME = 'isBl')
                    BEGIN
                        ALTER TABLE Upcoming ADD isBl BIT DEFAULT 0;
                    END
                    IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'Upcoming' AND COLUMN_NAME = 'Genre')
                    BEGIN
                        ALTER TABLE Upcoming ADD Genre NVARCHAR(50) DEFAULT 'Normal';
                    END
                    IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'Upcoming' AND COLUMN_NAME = 'Summary')
                    BEGIN
                        ALTER TABLE Upcoming ADD Summary NVARCHAR(MAX) NULL;
                    END
                    IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'Upcoming' AND COLUMN_NAME = 'ReleaseYear')
                    BEGIN
                        ALTER TABLE Upcoming ADD ReleaseYear NVARCHAR(50) NULL;
                    END
                    IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'Upcoming' AND COLUMN_NAME = 'Country')
                    BEGIN
                        ALTER TABLE Upcoming ADD Country NVARCHAR(50) NULL;
                    END
                    IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'Upcoming' AND COLUMN_NAME = 'Language')
                    BEGIN
                        ALTER TABLE Upcoming ADD [Language] NVARCHAR(50) NULL;
                    END
                    IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'Upcoming' AND COLUMN_NAME = 'Duration')
                    BEGIN
                        ALTER TABLE Upcoming ADD Duration NVARCHAR(50) NULL;
                    END
                    IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'Upcoming' AND COLUMN_NAME = 'Slug')
                    BEGIN
                        ALTER TABLE Upcoming ADD Slug NVARCHAR(255) NULL;
                    END
                    IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'Upcoming' AND COLUMN_NAME = 'CastData')
                    BEGIN
                        ALTER TABLE Upcoming ADD CastData NVARCHAR(MAX) NULL;
                    END
                    IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'Upcoming' AND COLUMN_NAME = 'Categories')
                    BEGIN
                        ALTER TABLE Upcoming ADD Categories NVARCHAR(500) NULL;
                    END
                    """);

            // Ensure GetUpcoming Stored Procedure exists and returns correct items and slugs
            jdbcTemplate.execute("""
                    CREATE OR ALTER PROCEDURE [dbo].[GetUpcoming]
                        @Category NVARCHAR(50) = NULL,
                        @Platform NVARCHAR(50) = NULL
                    AS
                    BEGIN
                        SET NOCOUNT ON;
                        SELECT
                            u.ID AS upcomingId,
                            COALESCE(u.ReferenceId, u.ID) AS ID,
                            u.Name AS [name],
                            u.[Type] AS [type],
                            u.[Status] AS [status],
                            u.[Datetime] AS [datetime],
                            u.Category,
                            u.Categories,
                            ISNULL(u.isBl, 0) AS isBl,
                            ISNULL(u.Genre, 'Normal') AS Genre,
                            COALESCE(u.Slug, s.slug, m.slug, e.slug) AS slug
                        FROM Upcoming u
                        LEFT JOIN Series s ON u.ReferenceId = s.ID
                        LEFT JOIN Movie m ON u.ReferenceId = m.ID
                        LEFT JOIN Episode e ON u.ReferenceId = e.ID
                        LEFT JOIN Series es ON e.SeriesId = es.ID
                        WHERE (@Category IS NULL OR u.Category = @Category)
                          AND (
                              @Platform IS NULL 
                              OR (@Platform = 'bldizi' AND (
                                  u.Genre IN ('BL', 'GL') OR u.isBl = 1
                                  OR s.SeriesType IN ('BL', 'GL') OR s.isBl = 1 OR EXISTS (SELECT 1 FROM SeriesCategories sc WHERE sc.SeriesID = s.ID AND sc.CategoryID IN (52, 63))
                                  OR m.MovieType IN ('BL', 'GL') OR m.isBl = 1 OR EXISTS (SELECT 1 FROM MovieCategories mc WHERE mc.MovieID = m.ID AND mc.CategoryID IN (52, 63))
                                  OR es.SeriesType IN ('BL', 'GL') OR es.isBl = 1 OR EXISTS (SELECT 1 FROM SeriesCategories esc WHERE esc.SeriesID = es.ID AND esc.CategoryID IN (52, 63))
                              ))
                              OR (@Platform = 'whodatidols' AND (
                                  (u.Genre IS NULL OR u.Genre NOT IN ('BL', 'GL')) AND (u.isBl = 0 OR u.isBl IS NULL)
                                  AND (s.ID IS NULL OR ((s.isBl = 0 OR s.isBl IS NULL) AND (s.SeriesType IS NULL OR s.SeriesType NOT IN ('BL', 'GL')) AND NOT EXISTS (SELECT 1 FROM SeriesCategories sc WHERE sc.SeriesID = s.ID AND sc.CategoryID IN (52, 63))))
                                  AND (m.ID IS NULL OR ((m.isBl = 0 OR m.isBl IS NULL) AND (m.MovieType IS NULL OR m.MovieType NOT IN ('BL', 'GL')) AND NOT EXISTS (SELECT 1 FROM MovieCategories mc WHERE mc.MovieID = m.ID AND mc.CategoryID IN (52, 63))))
                                  AND (es.ID IS NULL OR ((es.isBl = 0 OR es.isBl IS NULL) AND (es.SeriesType IS NULL OR es.SeriesType NOT IN ('BL', 'GL')) AND NOT EXISTS (SELECT 1 FROM SeriesCategories esc WHERE esc.SeriesID = es.ID AND esc.CategoryID IN (52, 63))))
                              ))
                          )
                        ORDER BY u.[datetime];
                    END
                    """);

            // Ensure AddUpcomingCustom Stored Procedure supports all parameters
            jdbcTemplate.execute("""
                    CREATE OR ALTER PROCEDURE [dbo].[AddUpcomingCustom]
                        @ID UNIQUEIDENTIFIER,
                        @Name NVARCHAR(255),
                        @Type NVARCHAR(50),
                        @Status NVARCHAR(100),
                        @Datetime DATETIME,
                        @ReferenceId UNIQUEIDENTIFIER = NULL,
                        @Category NVARCHAR(50) = 'Upcoming',
                        @Genre NVARCHAR(50) = 'Normal',
                        @IsBl BIT = 0,
                        @Summary NVARCHAR(MAX) = NULL,
                        @ReleaseYear NVARCHAR(50) = NULL,
                        @Country NVARCHAR(50) = NULL,
                        @Language NVARCHAR(50) = NULL,
                        @Duration NVARCHAR(50) = NULL,
                        @Slug NVARCHAR(255) = NULL,
                        @CastData NVARCHAR(MAX) = NULL,
                        @Categories NVARCHAR(500) = NULL
                    AS
                    BEGIN
                        SET NOCOUNT ON;
                        INSERT INTO Upcoming (ID, Name, [Type], [Status], [Datetime], ReferenceId, Category, Genre, isBl, Summary, ReleaseYear, Country, [Language], Duration, Slug, CastData, Categories)
                        VALUES (@ID, @Name, @Type, @Status, @Datetime, @ReferenceId, @Category, @Genre, @IsBl, @Summary, @ReleaseYear, @Country, @Language, @Duration, @Slug, @CastData, @Categories);
                    END
                    """);

            // Ensure DeleteUpcoming Stored Procedure exists and supports comment transfer
            jdbcTemplate.execute("""
                    CREATE OR ALTER PROCEDURE [dbo].[DeleteUpcoming]
                        @ID UNIQUEIDENTIFIER,
                        @TargetContentId UNIQUEIDENTIFIER = NULL
                    AS
                    BEGIN
                        SET NOCOUNT ON;

                        UPDATE c
                        SET c.ContentId = COALESCE(
                            @TargetContentId,
                            u.ReferenceId,
                            (SELECT TOP 1 e.ID FROM Episode e JOIN Series s ON e.SeriesId = s.ID WHERE s.Name = u.Name AND e.SeasonNumber = 1 AND e.EpisodeNumber = 1),
                            (SELECT TOP 1 s.ID FROM Series s WHERE s.Name = u.Name),
                            (SELECT TOP 1 m.ID FROM Movie m WHERE m.Name = u.Name),
                            c.ContentId
                        )
                        FROM Comments c
                        JOIN Upcoming u ON c.ContentId = u.ID
                        WHERE u.ID = @ID;

                        DELETE FROM Upcoming WHERE ID = @ID;
                    END
                    """);
        } catch (Exception e) {
            logger.error("Failed to ensure Upcoming procedure and schema", e);
        }
    }

    private void initDefaults() {
        if (getValue("announcement_text") == null) {
            // Default announcement text from existing HTML
            String defaultText = "DUYURU: Sitemizde bulunan \\\"BL Drama Turkey, Asian Drama House, Asian Drama Alarm\\\" içerikleri kendi sitemiz ve ortak olduğumuz sitelerdir. Çalma veya emek hırsızlığı gibi bir durum yoktur!";
            setValue("announcement_text", defaultText);
        }

        if (getValue("announcement_active") == null) {
            // Default to true (active)
            setValue("announcement_active", "true");
        }

        if (getValue("maintenance_mode") == null) {
            // Default to false (production active)
            setValue("maintenance_mode", "false");
        }

        if (getValue("maintenance_mode_wdi") == null) {
            String existing = getValue("maintenance_mode");
            setValue("maintenance_mode_wdi", existing != null ? existing : "false");
        }

        if (getValue("maintenance_mode_bldizi") == null) {
            setValue("maintenance_mode_bldizi", "false");
        }

        if (getValue("registration_enabled") == null) {
            // Default to true (registration open)
            setValue("registration_enabled", "true");
        }

        if (getValue("main_video_source_enabled") == null) {
            // Default to true (enabled)
            setValue("main_video_source_enabled", "true");
        }

        if (getValue("ad_probability") == null) {
            // Default ad display probability to 30%
            setValue("ad_probability", "30");
        }

        if (getValue("ad_video_enabled") == null) setValue("ad_video_enabled", "true");
        if (getValue("ad_socialbar_enabled") == null) setValue("ad_socialbar_enabled", "true");
        if (getValue("ad_skyscraper_enabled") == null) setValue("ad_skyscraper_enabled", "true");
        if (getValue("ad_banner_enabled") == null) setValue("ad_banner_enabled", "true");
        if (getValue("ad_support_btn_enabled") == null) setValue("ad_support_btn_enabled", "true");
    }

    public String getValue(String key) {
        return settingsCache.get(key);
    }

    public void setValue(String key, String value) {
        try {
            // Update if exists
            int updated = jdbcTemplate.update("UPDATE SystemSettings SET SettingValue = ? WHERE SettingKey = ?", value,
                    key);

            // Insert if not exists
            if (updated == 0) {
                jdbcTemplate.update("INSERT INTO SystemSettings (SettingKey, SettingValue) VALUES (?, ?)", key, value);
            }

            // Critical: Update cache immediately
            settingsCache.put(key, value != null ? value : "");
        } catch (Exception e) {
            logger.error("Error setting value for: {}", key, e);
        }
    }

    // Specific helper methods for Announcement
    public Map<String, Object> getAnnouncement() {
        Map<String, Object> result = new HashMap<>();
        String text = getValue("announcement_text");
        String activeStr = getValue("announcement_active");

        result.put("text", text != null ? text : "");
        result.put("active", "true".equalsIgnoreCase(activeStr));
        return result;
    }

    public void updateAnnouncement(String text, boolean active) {
        setValue("announcement_text", text);
        setValue("announcement_active", String.valueOf(active));
    }

    public boolean isMaintenanceMode() {
        return isMaintenanceModeWdi();
    }

    public boolean isMaintenanceModeWdi() {
        String val = getValue("maintenance_mode_wdi");
        if (val == null) {
            val = getValue("maintenance_mode");
        }
        return "true".equalsIgnoreCase(val);
    }

    public void setMaintenanceModeWdi(boolean active) {
        setValue("maintenance_mode_wdi", String.valueOf(active));
        setValue("maintenance_mode", String.valueOf(active));
    }

    public boolean isMaintenanceModeBlDizi() {
        String val = getValue("maintenance_mode_bldizi");
        return "true".equalsIgnoreCase(val);
    }

    public void setMaintenanceModeBlDizi(boolean active) {
        setValue("maintenance_mode_bldizi", String.valueOf(active));
    }

    public void setMaintenanceMode(boolean active) {
        setMaintenanceModeWdi(active);
    }

    public boolean isRegistrationEnabled() {
        String val = getValue("registration_enabled");
        return "true".equalsIgnoreCase(val);
    }

    public void setRegistrationEnabled(boolean active) {
        setValue("registration_enabled", String.valueOf(active));
    }

    public boolean isMainVideoSourceEnabled() {
        String val = getValue("main_video_source_enabled");
        return "true".equalsIgnoreCase(val);
    }

    public void setMainVideoSourceEnabled(boolean enabled) {
        setValue("main_video_source_enabled", String.valueOf(enabled));
    }

    public int getAdProbability() {
        String val = getValue("ad_probability");
        if (val == null) {
            return 30;
        }
        try {
            int p = Integer.parseInt(val.trim());
            return Math.max(0, Math.min(100, p));
        } catch (NumberFormatException e) {
            return 30;
        }
    }

    public void setAdProbability(int probability) {
        int clamped = Math.max(0, Math.min(100, probability));
        setValue("ad_probability", String.valueOf(clamped));
    }

    // ==========================================
    // REKLAM TURLERI ACMA / KAPATMA AYARLARI
    // ==========================================
    public boolean isAdVideoEnabled() {
        String val = getValue("ad_video_enabled");
        return val == null || "true".equalsIgnoreCase(val.trim());
    }

    public void setAdVideoEnabled(boolean enabled) {
        setValue("ad_video_enabled", String.valueOf(enabled));
    }

    public boolean isAdSocialBarEnabled() {
        String val = getValue("ad_socialbar_enabled");
        return val == null || "true".equalsIgnoreCase(val.trim());
    }

    public void setAdSocialBarEnabled(boolean enabled) {
        setValue("ad_socialbar_enabled", String.valueOf(enabled));
    }

    public boolean isAdSkyscraperEnabled() {
        String val = getValue("ad_skyscraper_enabled");
        return val == null || "true".equalsIgnoreCase(val.trim());
    }

    public void setAdSkyscraperEnabled(boolean enabled) {
        setValue("ad_skyscraper_enabled", String.valueOf(enabled));
    }

    public boolean isAdBannerEnabled() {
        String val = getValue("ad_banner_enabled");
        return val == null || "true".equalsIgnoreCase(val.trim());
    }

    public void setAdBannerEnabled(boolean enabled) {
        setValue("ad_banner_enabled", String.valueOf(enabled));
    }

    public boolean isAdSupportBtnEnabled() {
        String val = getValue("ad_support_btn_enabled");
        return val == null || "true".equalsIgnoreCase(val.trim());
    }

    public void setAdSupportBtnEnabled(boolean enabled) {
        setValue("ad_support_btn_enabled", String.valueOf(enabled));
    }

    public Map<String, Object> getAdSettings() {
        Map<String, Object> map = new HashMap<>();
        map.put("videoAds", isAdVideoEnabled());
        map.put("socialBar", isAdSocialBarEnabled());
        map.put("skyscraperAds", isAdSkyscraperEnabled());
        map.put("bannerAds", isAdBannerEnabled());
        map.put("supportButton", isAdSupportBtnEnabled());
        map.put("probability", getAdProbability());
        return map;
    }
}

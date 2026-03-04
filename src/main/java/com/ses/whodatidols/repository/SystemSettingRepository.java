package com.ses.whodatidols.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
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
    }

    private void initDefaults() {
        if (getValue("announcement_text") == null) {
            // Default announcement text from existing HTML
            String defaultText = "DUYURU: Sitemizde bulunan \"BL Drama Turkey, Asian Drama House, Asian Drama Alarm\" içerikleri kendi sitemiz ve ortak olduğumuz sitelerdir. Çalma veya emek hırsızlığı gibi bir durum yoktur!";
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

        if (getValue("registration_enabled") == null) {
            // Default to true (registration open)
            setValue("registration_enabled", "true");
        }
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
            logger.error("Error setting value for: " + key, e);
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
        String val = getValue("maintenance_mode");
        return "true".equalsIgnoreCase(val);
    }

    public void setMaintenanceMode(boolean active) {
        setValue("maintenance_mode", String.valueOf(active));
    }

    public boolean isRegistrationEnabled() {
        String val = getValue("registration_enabled");
        return "true".equalsIgnoreCase(val);
    }

    public void setRegistrationEnabled(boolean active) {
        setValue("registration_enabled", String.valueOf(active));
    }
}

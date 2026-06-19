package com.ses.whodatidols.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class DatabaseMaintenanceService {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseMaintenanceService.class);
    private final JdbcTemplate jdbcTemplate;

    public DatabaseMaintenanceService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Cleans up view logs older than 30 days every day at midnight (00:00:00).
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void cleanupOldViewLogs() {
        logger.info("Starting scheduled cleanup of old content view logs...");
        try {
            int deletedRows = jdbcTemplate.update(
                    "DELETE FROM [dbo].[ContentViewLog] WHERE [ViewedAt] < DATEADD(day, -30, GETDATE())");
            logger.info("Cleanup finished. Deleted {} log rows older than 30 days.", deletedRows);
        } catch (Exception e) {
            logger.error("Error occurred during content view log cleanup: {}", e.getMessage(), e);
        }
    }
}

package com.ses.whodatidols.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class TrafficStatsService {
    private static final Logger logger = LoggerFactory.getLogger(TrafficStatsService.class);
    
    // Application-level fallback (Windows or internal tracking)
    private final AtomicLong totalBytesInLastSecond = new AtomicLong(0);
    
    // System-level tracking (Linux)
    private long lastTotalTxBytes = -1;
    
    private double currentMbps = 0.0;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final boolean isLinux;

    public TrafficStatsService() {
        this.isLinux = System.getProperty("os.name").toLowerCase().contains("linux");
        logger.info("TrafficStatsService started. OS: {}, Linux mode: {}", System.getProperty("os.name"), isLinux);
        
        // Calculate Mbps every second
        scheduler.scheduleAtFixedRate(this::calculateStats, 1, 1, TimeUnit.SECONDS);
    }

    /**
     * Records bytes served by the application.
     * Used as primary on Windows, and complementary data on Linux.
     */
    public void recordBytes(long bytes) {
        if (bytes > 0) {
            totalBytesInLastSecond.addAndGet(bytes);
        }
    }

    private void calculateStats() {
        try {
            double mbps = 0.0;
            
            if (isLinux) {
                mbps = calculateLinuxSystemMbps();
            } else {
                mbps = calculateFallbackMbps();
            }
            
            // Safety: Never negative
            this.currentMbps = Math.max(0.0, Math.round(mbps * 100.0) / 100.0);
            
        } catch (Exception e) {
            logger.error("Error calculating traffic stats: {}", e.getMessage());
        }
    }

    private double calculateFallbackMbps() {
        long bytes = totalBytesInLastSecond.getAndSet(0);
        return (bytes * 8.0) / (1024.0 * 1024.0);
    }

    /**
     * Reads /proc/net/dev to get total transmitted bytes across all network interfaces.
     */
    private double calculateLinuxSystemMbps() {
        try {
            long currentTotalTx = 0;
            List<String> lines = Files.readAllLines(Paths.get("/proc/net/dev"));
            
            // Skip first two header lines
            for (int i = 2; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty() || line.startsWith("lo:")) continue;
                
                String[] parts = line.split("\\s+");
                if (parts.length > 9) {
                    // TX bytes is typically at index 9 (10th column) in /proc/net/dev
                    // Format: face |bytes packets errs drop fifo frame compressed multicast|bytes ...
                    // Col 0: face, Col 1-8: RX, Col 9-16: TX
                    try {
                        currentTotalTx += Long.parseLong(parts[9]);
                    } catch (NumberFormatException e) {
                        // Ignore parse errors for specific lines
                    }
                }
            }
            
            if (lastTotalTxBytes == -1) {
                lastTotalTxBytes = currentTotalTx;
                return calculateFallbackMbps(); // Use app stats for the very first second
            }
            
            long deltaBytes = currentTotalTx - lastTotalTxBytes;
            lastTotalTxBytes = currentTotalTx;
            
            // If delta is negative (counter rollover), fallback
            if (deltaBytes < 0) return calculateFallbackMbps();
            
            // Total system Mbps
            return (deltaBytes * 8.0) / (1024.0 * 1024.0);
            
        } catch (IOException e) {
            logger.warn("Could not read /proc/net/dev, falling back to application tracking");
            return calculateFallbackMbps();
        }
    }

    public double getCurrentMbps() {
        return currentMbps;
    }
    
    public void shutdown() {
        scheduler.shutdown();
    }
}

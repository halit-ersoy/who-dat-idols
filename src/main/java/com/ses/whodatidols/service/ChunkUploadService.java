package com.ses.whodatidols.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service to handle chunked / sliced file uploads for bypassing Cloudflare's 100MB body limit.
 */
@Service
public class ChunkUploadService {

    private static final Logger logger = LoggerFactory.getLogger(ChunkUploadService.class);

    @Value("${media.source.movies.path:}")
    private String moviesPath;

    private final Map<String, Object> uploadLocks = new ConcurrentHashMap<>();

    private Path getTempBaseDir() {
        Path base;
        if (moviesPath != null && !moviesPath.isBlank()) {
            base = Paths.get(moviesPath).resolve("temp_chunks").toAbsolutePath().normalize();
        } else {
            base = Paths.get(System.getProperty("java.io.tmpdir"), "wdi_chunks").toAbsolutePath().normalize();
        }
        try {
            if (!Files.exists(base)) {
                Files.createDirectories(base);
            }
        } catch (IOException e) {
            logger.error("Failed to create temp chunks dir: {}", e.getMessage());
        }
        return base;
    }

    /**
     * Store an incoming chunk. If it is the last chunk and all chunks are present, merge into one file.
     * @return Map containing success, completed status, and fileKey if merge completed.
     */
    public Map<String, Object> saveChunk(String uploadId, int chunkIndex, int totalChunks, String fileName, MultipartFile chunk) throws IOException {
        if (uploadId == null || uploadId.isBlank()) {
            throw new IllegalArgumentException("uploadId cannot be blank");
        }
        if (chunk == null || chunk.isEmpty()) {
            throw new IllegalArgumentException("chunk file cannot be empty");
        }

        // Sanitize uploadId and fileName to prevent directory traversal
        String safeUploadId = uploadId.replaceAll("[^a-zA-Z0-9_-]", "");
        String safeFileName = (fileName != null && !fileName.isBlank())
                ? Paths.get(fileName).getFileName().toString().replaceAll("[^a-zA-Z0-9._-]", "_")
                : "video.mp4";

        Path uploadDir = getTempBaseDir().resolve(safeUploadId);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        Path chunkFile = uploadDir.resolve(chunkIndex + ".part");
        chunk.transferTo(chunkFile);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("chunkIndex", chunkIndex);
        response.put("totalChunks", totalChunks);

        // Check if all chunks (0 .. totalChunks - 1) have arrived
        boolean allChunksReady = true;
        for (int i = 0; i < totalChunks; i++) {
            if (!Files.exists(uploadDir.resolve(i + ".part"))) {
                allChunksReady = false;
                break;
            }
        }

        if (allChunksReady) {
            Object lock = uploadLocks.computeIfAbsent(safeUploadId, k -> new Object());
            synchronized (lock) {
                Path mergedFile = uploadDir.resolve(safeFileName);
                if (!Files.exists(mergedFile)) {
                    logger.info("Merging {} chunks for uploadId: {} -> {}", totalChunks, safeUploadId, safeFileName);
                    try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(mergedFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE))) {
                        byte[] buffer = new byte[64 * 1024];
                        for (int i = 0; i < totalChunks; i++) {
                            Path part = uploadDir.resolve(i + ".part");
                            try (InputStream in = new BufferedInputStream(Files.newInputStream(part))) {
                                int bytesRead;
                                while ((bytesRead = in.read(buffer)) != -1) {
                                    out.write(buffer, 0, bytesRead);
                                }
                            }
                        }
                    }

                    // Delete .part files after successful merge
                    for (int i = 0; i < totalChunks; i++) {
                        try {
                            Files.deleteIfExists(uploadDir.resolve(i + ".part"));
                        } catch (Exception ignored) {}
                    }
                    uploadLocks.remove(safeUploadId);
                }

                String fileKey = safeUploadId + "/" + safeFileName;
                response.put("completed", true);
                response.put("fileKey", fileKey);
                response.put("fileName", safeFileName);
                response.put("fileSize", Files.size(mergedFile));
                return response;
            }
        }

        response.put("completed", false);
        return response;
    }

    /**
     * Resolves the merged file by fileKey (format: "{uploadId}/{fileName}")
     */
    public File getMergedFile(String fileKey) {
        if (fileKey == null || fileKey.isBlank()) return null;
        String[] parts = fileKey.replace('\\', '/').split("/");
        if (parts.length != 2) return null;
        String safeUploadId = parts[0].replaceAll("[^a-zA-Z0-9_-]", "");
        String safeFileName = parts[1].replaceAll("[^a-zA-Z0-9._-]", "_");

        Path filePath = getTempBaseDir().resolve(safeUploadId).resolve(safeFileName);
        File file = filePath.toFile();
        return file.exists() ? file : null;
    }

    /**
     * Cleans up the temporary directory for an uploadId
     */
    public void cleanup(String fileKey) {
        try {
            File f = getMergedFile(fileKey);
            if (f != null && f.getParentFile() != null) {
                File dir = f.getParentFile();
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File child : files) {
                        try { child.delete(); } catch (Exception ignored) {}
                    }
                }
                dir.delete();
            }
        } catch (Exception e) {
            logger.warn("Cleanup error for {}: {}", fileKey, e.getMessage());
        }
    }
}

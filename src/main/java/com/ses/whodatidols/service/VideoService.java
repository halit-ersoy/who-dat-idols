package com.ses.whodatidols.service;

import com.ses.whodatidols.repository.ContentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class VideoService {
    private final ContentRepository contentRepository;

    public VideoService(ContentRepository contentRepository) {
        this.contentRepository = contentRepository;
    }

    public String getVideoPath(UUID id) {
        return contentRepository.findVideoUrlById(id);
    }

    public void incrementViewCount(UUID id) {
        contentRepository.incrementViewCount(id);
    }

    public ResponseEntity<StreamingResponseBody> streamVideo(UUID id, String rangeHeader) {
        try {
            String videoPath = getVideoPath(id);
            if (videoPath == null || videoPath.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            File videoFile = new File(videoPath);
            long fileLength = videoFile.length();
            long start = 0;
            long end = fileLength - 1;

            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                String[] ranges = rangeHeader.replace("bytes=", "").split("-");
                try {
                    start = Long.parseLong(ranges[0]);
                    if (ranges.length > 1 && !ranges[1].isEmpty()) {
                        end = Long.parseLong(ranges[1]);
                    }
                } catch (NumberFormatException ignored) {
                }
            }

            long contentLength = end - start + 1;
            InputStream inputStream = new FileInputStream(videoFile);
            inputStream.skip(start);

            StreamingResponseBody responseBody = outputStream -> {
                byte[] buffer = new byte[8192];
                long bytesToRead = contentLength;
                int bytesRead;
                while (bytesToRead > 0 && (bytesRead = inputStream.read(buffer, 0, (int) Math.min(buffer.length, bytesToRead))) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    bytesToRead -= bytesRead;
                }
                inputStream.close();
            };

            String contentRange = String.format("bytes %d-%d/%d", start, end, fileLength);
            return ResponseEntity.status(rangeHeader != null ? 206 : 200)
                    .header("Content-Type", "video/mp4")
                    .header("Accept-Ranges", "bytes")
                    .header("Content-Length", String.valueOf(contentLength))
                    .header("Content-Range", contentRange)
                    .body(responseBody);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    // Add transcoding methods from your commented code
    public ResponseEntity<?> getTranscodedVideo(int videoId, int targetRes, String rangeHeader) {
        try {
            // Implementation based on your commented code
            // ...
            return ResponseEntity.ok().build(); // Placeholder
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
}
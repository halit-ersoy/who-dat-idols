package com.ses.whodatidols.service;

import com.ses.whodatidols.util.FFmpegUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Service
public class TranscodingService {
    private final Map<String, String> transcodedCache = new HashMap<>();

    public ResourceRegion resourceRegion(Resource video, String rangeHeader, long contentLength) throws IOException {
        long chunkSize = 1024 * 1024; // 1 MB

        if (rangeHeader == null) {
            long rangeLength = Math.min(chunkSize, contentLength);
            return new ResourceRegion(video, 0, rangeLength);
        } else {
            String[] ranges = rangeHeader.replace("bytes=", "").split("-");
            long start = Long.parseLong(ranges[0]);
            long end = (ranges.length > 1 && StringUtils.hasLength(ranges[1]))
                    ? Long.parseLong(ranges[1])
                    : contentLength - 1;
            long rangeLength = Math.min(chunkSize, end - start + 1);
            return new ResourceRegion(video, start, rangeLength);
        }
    }

    public ResponseEntity<ResourceRegion> getTranscodedVideo(String originalVideoPath, String videoId,
                                                             int targetRes, String rangeHeader) throws IOException {
        try {
            int[] dimensions = FFmpegUtils.getVideoWidthHeight(originalVideoPath);
            if (dimensions == null) {
                return ResponseEntity.status(500).build();
            }

            int originalWidth = dimensions[0];
            int originalHeight = dimensions[1];

            int maxDimension = Math.max(originalWidth, originalHeight);
            int actualTargetRes = (targetRes > maxDimension) ? maxDimension : targetRes;

            int newHeight = actualTargetRes;
            int newWidth = (int) ((double) originalWidth / (double) originalHeight * newHeight);

            String cacheKey = videoId + "-" + actualTargetRes;
            String transcodedPath = transcodedCache.get(cacheKey);

            if (transcodedPath == null || !Files.exists(Paths.get(transcodedPath))) {
                String tempDir = System.getProperty("java.io.tmpdir");
                String outFile = tempDir + File.separator + "transcoded_" + videoId + "_" + actualTargetRes + ".mp4";

                try {
                    FFmpegUtils.transcodeVideo(originalVideoPath, outFile, newWidth, newHeight);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return ResponseEntity.status(500).body(null);
                }

                transcodedCache.put(cacheKey, outFile);
                transcodedPath = outFile;
            }

            FileSystemResource videoResource = new FileSystemResource(transcodedPath);
            if (!videoResource.exists()) {
                return ResponseEntity.notFound().build();
            }

            long contentLength = videoResource.contentLength();
            ResourceRegion region = resourceRegion(videoResource, rangeHeader, contentLength);

            MediaType mediaType = MediaTypeFactory
                    .getMediaType(videoResource)
                    .orElse(MediaType.APPLICATION_OCTET_STREAM);

            return ResponseEntity
                    .status(HttpStatus.PARTIAL_CONTENT)
                    .contentType(mediaType)
                    .body(region);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }
}
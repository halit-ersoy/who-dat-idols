package com.ses.whodatidols.service;

import com.ses.whodatidols.repository.ContentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class VideoService {
    private final ContentRepository contentRepository;

    @Value("${media.source.movies.path}")
    private String moviesPath;

    @Value("${media.source.soap_operas.path}")
    private String soapOperasPath;

    @Value("${media.source.trailers.path}")
    private String trailersPath;

    public VideoService(ContentRepository contentRepository) {
        this.contentRepository = contentRepository;
    }

    public String getVideoPath(UUID id) {
        String type = contentRepository.findVideoUrlById(id);
        if (type == null || "unknown".equalsIgnoreCase(type)) {
            return null;
        }

        java.nio.file.Path basePath;
        switch (type.toLowerCase()) {
            case "movie":
                basePath = java.nio.file.Paths.get(moviesPath);
                break;
            case "soap_opera":
            case "soap-opera":
            case "soapopera":
            case "series":
                basePath = java.nio.file.Paths.get(soapOperasPath);
                break;
            case "trailer":
                basePath = java.nio.file.Paths.get(trailersPath);
                break;
            default:
                return null;
        }

        java.nio.file.Path videoPath = basePath.resolve(id + ".mp4").toAbsolutePath().normalize();
        return videoPath.toString();
    }

    public void incrementViewCount(UUID id) {
        contentRepository.incrementViewCount(id);
    }

}
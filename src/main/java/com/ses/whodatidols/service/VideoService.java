package com.ses.whodatidols.service;

import com.ses.whodatidols.repository.ContentRepository;
import org.springframework.stereotype.Service;
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

}
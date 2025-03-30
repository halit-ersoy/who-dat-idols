package com.ses.whodatidols.controller;

import com.ses.whodatidols.service.TranscodingService;
import com.ses.whodatidols.service.VideoService;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.UUID;

@RestController
@RequestMapping("/api/video")
public class VideoController {
    private final VideoService videoService;
    private final TranscodingService transcodingService;

    public VideoController(VideoService videoService, TranscodingService transcodingService) {
        this.videoService = videoService;
        this.transcodingService = transcodingService;
    }

    @GetMapping("/stream")
    public ResponseEntity<StreamingResponseBody> streamVideo(
            @RequestParam("id") UUID id,
            @RequestHeader(value = "Range", required = false) String rangeHeader) {
        return videoService.streamVideo(id, rangeHeader);
    }

    @GetMapping("/transcode")
    public ResponseEntity<ResourceRegion> getTranscodedVideo(
            @RequestParam("id") UUID id,
            @RequestParam(value = "res", defaultValue = "720") int resolution,
            @RequestHeader(value = "Range", required = false) String rangeHeader) throws Exception {
        String originalPath = videoService.getVideoPath(id);
        return transcodingService.getTranscodedVideo(originalPath, String.valueOf(id), resolution, rangeHeader);
    }
}
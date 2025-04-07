package com.ses.whodatidols.controller;

import com.ses.whodatidols.service.TranscodingService;
import com.ses.whodatidols.service.VideoService;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/video")
public class VideoController {
    private final VideoService videoService;
    private final TranscodingService transcodingService;
    private final JdbcTemplate jdbcTemplate;

    public VideoController(VideoService videoService, TranscodingService transcodingService, JdbcTemplate jdbcTemplate) {
        this.videoService = videoService;
        this.transcodingService = transcodingService;
        this.jdbcTemplate = jdbcTemplate;
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

    @PostMapping("/comment")
    public ResponseEntity<?> addComment(
            @RequestParam("id") UUID id,
            @CookieValue(value = "wdiAuth", required = false) String cookie,
            @RequestParam(value = "spoiler", defaultValue = "false") boolean spoiler,
            @RequestBody String commentText) {

        if (cookie == null || cookie.trim().isEmpty()) {
            return ResponseEntity.status(401).body("Unauthorized: Login required to comment");
        }

        if (commentText == null || commentText.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Comment text cannot be empty");
        }

        try {
            String sql = "{call AddOrUpdateComment(?, ?, ?, ?, ?)}";
            jdbcTemplate.update(sql,
                    id,                          // @ID
                    cookie,                      // @cookie
                    new java.sql.Timestamp(System.currentTimeMillis()), // @datetime
                    spoiler ? 1 : 0,            // @spoiler
                    commentText                 // @comment
            );

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to add comment: " + e.getMessage());
        }
    }

    @GetMapping("/comments")
    public ResponseEntity<?> getComments(@RequestParam("id") UUID id) {
        try {
            String sql = "EXEC GetComments @ID = ?";
            List<Map<String, Object>> comments = jdbcTemplate.queryForList(sql, id.toString());
            return ResponseEntity.ok(comments);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to retrieve comments: " + e.getMessage());
        }
    }

    @PostMapping("/increment-view")
    public ResponseEntity<?> incrementViewCount(@RequestParam("id") UUID id) {
        try {
            videoService.incrementViewCount(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to increment view count");
        }
    }
}
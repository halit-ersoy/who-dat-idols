package com.ses.whodatidols.controller;

import com.ses.whodatidols.repository.CommentRepository;
import com.ses.whodatidols.service.WebPushOutboxService;
import com.ses.whodatidols.viewmodel.CommentViewModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/video")
public class CommentController {

    private final CommentRepository commentRepository;
    private final WebPushOutboxService webPushOutboxService;

    public CommentController(CommentRepository commentRepository, WebPushOutboxService webPushOutboxService) {
        this.commentRepository = commentRepository;
        this.webPushOutboxService = webPushOutboxService;
    }

    @GetMapping("/comments")
    public ResponseEntity<List<CommentViewModel>> getComments(
            @RequestParam("id") String id,
            @CookieValue(value = "wdiAuth", required = false) String cookie) {
        try {
            return ResponseEntity.ok(commentRepository.getComments(id, cookie));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/comment")
    public ResponseEntity<Void> addComment(
            @RequestParam("id") String id,
            @RequestParam(value = "spoiler", defaultValue = "false") boolean spoiler,
            @RequestParam(value = "parentId", required = false) UUID parentId,
            @RequestBody String text,
            @CookieValue(name = "wdiAuth", required = false) String cookie) {

        if (cookie == null || cookie.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            UUID newCommentId = commentRepository.addComment(id, cookie, text, spoiler, parentId);
            if (parentId != null) {
                // If comment is auto-approved (e.g. staff), this enqueues push; if not approved yet, it silently returns
                webPushOutboxService.enqueueCommentReplyNotification(newCommentId);
            }
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("Unauthorized or Banned")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/comment/like")
    public ResponseEntity<Void> likeComment(
            @RequestParam("commentId") UUID commentId,
            @CookieValue(name = "wdiAuth", required = false) String cookie) {

        if (cookie == null || cookie.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            commentRepository.likeComment(commentId, cookie);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/comment")
    public ResponseEntity<Void> deleteComment(
            @RequestParam("commentId") UUID commentId,
            @CookieValue(name = "wdiAuth", required = false) String cookie) {

        if (cookie == null || cookie.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            boolean deleted = commentRepository.deleteComment(commentId, cookie);
            if (deleted) {
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

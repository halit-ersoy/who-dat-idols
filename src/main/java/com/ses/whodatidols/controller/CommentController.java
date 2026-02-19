package com.ses.whodatidols.controller;

import com.ses.whodatidols.repository.CommentRepository;
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

    public CommentController(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    @GetMapping("/comments")
    public ResponseEntity<List<CommentViewModel>> getComments(
            @RequestParam("id") UUID id,
            @CookieValue(value = "wdiAuth", required = false) String cookie) {
        try {
            return ResponseEntity.ok(commentRepository.getComments(id, cookie));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/comment")
    public ResponseEntity<Void> addComment(
            @RequestParam("id") UUID id,
            @RequestParam(value = "spoiler", defaultValue = "false") boolean spoiler,
            @RequestParam(value = "parentId", required = false) UUID parentId,
            @RequestBody String text,
            @CookieValue(name = "wdiAuth", required = false) String cookie) {

        if (cookie == null || cookie.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            commentRepository.addComment(id, cookie, text, spoiler, parentId);
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
            if (e.getMessage() != null && e.getMessage().contains("Unauthorized or Banned")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
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
            if (!deleted) {
                // If deletion failed, it's either because:
                // 1. Comment doesn't exist (handled by queryForObject exception in repo)
                // 2. User doesn't own it
                // We'll return 403 as the most likely security reason if authorization failed.
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

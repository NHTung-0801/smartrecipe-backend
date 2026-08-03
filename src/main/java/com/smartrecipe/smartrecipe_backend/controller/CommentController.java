package com.smartrecipe.smartrecipe_backend.controller;

import com.smartrecipe.smartrecipe_backend.dto.request.CommentRequest;
import com.smartrecipe.smartrecipe_backend.dto.response.CommentResponse;
import com.smartrecipe.smartrecipe_backend.exception.ResourceNotFoundException;
import com.smartrecipe.smartrecipe_backend.repository.UserRepository;
import com.smartrecipe.smartrecipe_backend.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final UserRepository userRepository;

    private Long getUserId(Principal principal) {
        return userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"))
                .getId();
    }

    // ==================== COMMENT CRUD ====================

    @GetMapping("/recipes/{recipeId}/comments")
    public ResponseEntity<List<CommentResponse>> getComments(
            @PathVariable Long recipeId) {
        List<CommentResponse> comments = commentService.getCommentsByRecipeId(recipeId);
        return ResponseEntity.ok(comments);
    }

    @PostMapping("/recipes/{recipeId}/comments")
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable Long recipeId,
            @Valid @RequestBody CommentRequest request,
            Principal principal) {
        Long userId = getUserId(principal);
        CommentResponse response = commentService.createComment(recipeId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/comments/{id}")
    public ResponseEntity<CommentResponse> updateComment(
            @PathVariable Long id,
            @Valid @RequestBody CommentRequest request,
            Principal principal) {
        Long userId = getUserId(principal);
        CommentResponse response = commentService.updateComment(id, request, userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/comments/{id}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long id,
            Principal principal) {
        Long userId = getUserId(principal);
        commentService.deleteComment(id, userId);
        return ResponseEntity.noContent().build();
    }
}
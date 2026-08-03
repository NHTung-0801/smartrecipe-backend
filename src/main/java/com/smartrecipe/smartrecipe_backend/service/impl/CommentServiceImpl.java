package com.smartrecipe.smartrecipe_backend.service.impl;

import com.smartrecipe.smartrecipe_backend.dto.request.CommentRequest;
import com.smartrecipe.smartrecipe_backend.dto.response.AuthorSummaryResponse;
import com.smartrecipe.smartrecipe_backend.dto.response.CommentResponse;
import com.smartrecipe.smartrecipe_backend.entity.Recipe;
import com.smartrecipe.smartrecipe_backend.entity.RecipeComment;
import com.smartrecipe.smartrecipe_backend.entity.User;
import com.smartrecipe.smartrecipe_backend.exception.ResourceNotFoundException;
import com.smartrecipe.smartrecipe_backend.repository.RecipeCommentRepository;
import com.smartrecipe.smartrecipe_backend.repository.RecipeRepository;
import com.smartrecipe.smartrecipe_backend.repository.UserRepository;
import com.smartrecipe.smartrecipe_backend.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentServiceImpl implements CommentService {

    private final RecipeCommentRepository commentRepository;
    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponse> getCommentsByRecipeId(Long recipeId) {
        // Kiểm tra recipe tồn tại
        if (!recipeRepository.existsById(recipeId)) {
            throw new ResourceNotFoundException("Không tìm thấy công thức với id: " + recipeId);
        }

        // Lấy các bình luận gốc (parent = null)
        List<RecipeComment> rootComments = commentRepository
                .findByRecipeIdAndParentIsNullOrderByCreatedAtDesc(recipeId);

        return rootComments.stream()
                .map(this::mapToCommentResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CommentResponse createComment(Long recipeId, CommentRequest request, Long userId) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công thức với id: " + recipeId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với id: " + userId));

        RecipeComment comment = RecipeComment.builder()
                .content(request.getContent())
                .recipe(recipe)
                .user(user)
                .build();

        // Nếu có parentId -> reply
        if (request.getParentId() != null) {
            RecipeComment parentComment = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bình luận cha với id: " + request.getParentId()));
            comment.setParent(parentComment);
        }

        RecipeComment savedComment = commentRepository.save(comment);
        return mapToCommentResponse(savedComment);
    }

    @Override
    public CommentResponse updateComment(Long commentId, CommentRequest request, Long userId) {
        RecipeComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bình luận với id: " + commentId));

        // Chỉ chủ sở hữu mới được sửa
        if (!comment.getUser().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền sửa bình luận này");
        }

        comment.setContent(request.getContent());
        RecipeComment updatedComment = commentRepository.save(comment);
        return mapToCommentResponse(updatedComment);
    }

    @Override
    public void deleteComment(Long commentId, Long userId) {
        RecipeComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bình luận với id: " + commentId));

        // Chỉ chủ sở hữu hoặc chủ công thức mới được xóa
        if (!comment.getUser().getId().equals(userId) &&
                !comment.getRecipe().getAuthor().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền xóa bình luận này");
        }

        commentRepository.delete(comment);
    }

    // Helper: map Entity -> DTO (đệ quy xử lý replies)
    private CommentResponse mapToCommentResponse(RecipeComment comment) {
        AuthorSummaryResponse author = AuthorSummaryResponse.builder()
                .id(comment.getUser().getId())
                .username(comment.getUser().getUsername())
                .displayName(comment.getUser().getDisplayName())
                .avatarUrl(comment.getUser().getAvatarUrl())
                .build();

        List<CommentResponse> replyResponses = comment.getReplies() != null
                ? comment.getReplies().stream()
                        .map(this::mapToCommentResponse)
                        .collect(Collectors.toList())
                : List.of();

        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .author(author)
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .replies(replyResponses)
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}
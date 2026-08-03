package com.smartrecipe.smartrecipe_backend.service;

import com.smartrecipe.smartrecipe_backend.dto.request.CommentRequest;
import com.smartrecipe.smartrecipe_backend.dto.response.CommentResponse;

import java.util.List;

public interface CommentService {

    // Lấy tất cả bình luận của recipe (dạng cây: root + replies lồng nhau)
    List<CommentResponse> getCommentsByRecipeId(Long recipeId);

    // Tạo bình luận (hoặc reply nếu có parentId)
    CommentResponse createComment(Long recipeId, CommentRequest request, Long userId);

    // Cập nhật bình luận (chỉ chủ sở hữu)
    CommentResponse updateComment(Long commentId, CommentRequest request, Long userId);

    // Xóa bình luận (chỉ chủ sở hữu)
    void deleteComment(Long commentId, Long userId);
}
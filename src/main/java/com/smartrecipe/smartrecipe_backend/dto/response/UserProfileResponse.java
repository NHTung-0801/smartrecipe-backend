package com.smartrecipe.smartrecipe_backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserProfileResponse {
    private Long id;
    private String username;
    private String email;
    private String displayName;
    private String avatarUrl;
    private String bio;
    
    // Các trường thống kê
    private Integer recipeCount;
    private Long followerCount;
    private Long followingCount;
    private Boolean isFollowing; // Dành cho Public Profile (đang xem có follow người này chưa)
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

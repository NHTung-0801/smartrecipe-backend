package com.smartrecipe.smartrecipe_backend.service;

import com.smartrecipe.smartrecipe_backend.dto.response.AuthorSummaryResponse;
import org.springframework.data.domain.Page;

public interface FollowService {
    void followUser(Long targetUserId, String currentUsername);
    
    void unfollowUser(Long targetUserId, String currentUsername);
    
    Page<AuthorSummaryResponse> getFollowers(Long userId, int page, int size);
    
    Page<AuthorSummaryResponse> getFollowing(Long userId, int page, int size);
}

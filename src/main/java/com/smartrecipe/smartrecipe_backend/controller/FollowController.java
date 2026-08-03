package com.smartrecipe.smartrecipe_backend.controller;

import com.smartrecipe.smartrecipe_backend.dto.response.ApiResponse;
import com.smartrecipe.smartrecipe_backend.dto.response.AuthorSummaryResponse;
import com.smartrecipe.smartrecipe_backend.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    @PostMapping("/{id}/follow")
    public ResponseEntity<ApiResponse<Void>> followUser(
            @PathVariable Long id,
            Authentication authentication) {
        String currentUsername = authentication.getName();
        followService.followUser(id, currentUsername);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã theo dõi người dùng thành công"));
    }

    @DeleteMapping("/{id}/follow")
    public ResponseEntity<ApiResponse<Void>> unfollowUser(
            @PathVariable Long id,
            Authentication authentication) {
        String currentUsername = authentication.getName();
        followService.unfollowUser(id, currentUsername);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã hủy theo dõi người dùng thành công"));
    }

    @GetMapping("/{id}/followers")
    public ResponseEntity<ApiResponse<Page<AuthorSummaryResponse>>> getFollowers(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<AuthorSummaryResponse> followers = followService.getFollowers(id, page, size);
        return ResponseEntity.ok(ApiResponse.success(followers, "Lấy danh sách người theo dõi thành công"));
    }

    @GetMapping("/{id}/following")
    public ResponseEntity<ApiResponse<Page<AuthorSummaryResponse>>> getFollowing(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<AuthorSummaryResponse> following = followService.getFollowing(id, page, size);
        return ResponseEntity.ok(ApiResponse.success(following, "Lấy danh sách người đang theo dõi thành công"));
    }
}

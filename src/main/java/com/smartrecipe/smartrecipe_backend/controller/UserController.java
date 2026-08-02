package com.smartrecipe.smartrecipe_backend.controller;

import com.smartrecipe.smartrecipe_backend.dto.request.ChangePasswordRequest;
import com.smartrecipe.smartrecipe_backend.dto.request.UpdateProfileRequest;
import com.smartrecipe.smartrecipe_backend.dto.response.ApiResponse;
import com.smartrecipe.smartrecipe_backend.dto.response.UserProfileResponse;
import com.smartrecipe.smartrecipe_backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile(Authentication authentication) {
        String username = authentication.getName();
        UserProfileResponse profile = userService.getUserProfile(username);
        return ResponseEntity.ok(ApiResponse.success(profile, "Lấy thông tin hồ sơ thành công"));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateMyProfile(
            Authentication authentication, 
            @Valid @RequestBody UpdateProfileRequest request) {
        String username = authentication.getName();
        UserProfileResponse updatedProfile = userService.updateProfile(username, request);
        return ResponseEntity.ok(ApiResponse.success(updatedProfile, "Cập nhật hồ sơ thành công"));
    }

    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            Authentication authentication, 
            @Valid @RequestBody ChangePasswordRequest request) {
        String username = authentication.getName();
        userService.changePassword(username, request);
        return ResponseEntity.ok(ApiResponse.success(null, "Đổi mật khẩu thành công"));
    }
}

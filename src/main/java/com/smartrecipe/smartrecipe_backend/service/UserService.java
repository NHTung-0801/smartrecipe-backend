package com.smartrecipe.smartrecipe_backend.service;

import com.smartrecipe.smartrecipe_backend.dto.request.ChangePasswordRequest;
import com.smartrecipe.smartrecipe_backend.dto.request.UpdateProfileRequest;
import com.smartrecipe.smartrecipe_backend.dto.response.UserProfileResponse;

public interface UserService {
    UserProfileResponse getUserProfile(String username);
    UserProfileResponse getPublicUserProfile(Long userId, String currentUsername);
    UserProfileResponse updateProfile(String username, UpdateProfileRequest request);
    void changePassword(String username, ChangePasswordRequest request);
    UserProfileResponse updateAvatar(String username, org.springframework.web.multipart.MultipartFile file);
}

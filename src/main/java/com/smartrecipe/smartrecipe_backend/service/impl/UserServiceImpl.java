package com.smartrecipe.smartrecipe_backend.service.impl;

import com.smartrecipe.smartrecipe_backend.dto.request.ChangePasswordRequest;
import com.smartrecipe.smartrecipe_backend.dto.request.UpdateProfileRequest;
import com.smartrecipe.smartrecipe_backend.dto.response.UserProfileResponse;
import com.smartrecipe.smartrecipe_backend.entity.User;
import com.smartrecipe.smartrecipe_backend.exception.BadRequestException;
import com.smartrecipe.smartrecipe_backend.exception.ResourceNotFoundException;
import com.smartrecipe.smartrecipe_backend.repository.UserRepository;
import com.smartrecipe.smartrecipe_backend.repository.FollowRepository;
import com.smartrecipe.smartrecipe_backend.repository.RecipeRepository;
import com.smartrecipe.smartrecipe_backend.enums.RecipeStatus;
import com.smartrecipe.smartrecipe_backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final RecipeRepository recipeRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.smartrecipe.smartrecipe_backend.service.CloudinaryService cloudinaryService;

    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
    }

    private UserProfileResponse mapToResponse(User user, String currentUsername) {
        long followerCount = followRepository.countByFollowingId(user.getId());
        long followingCount = followRepository.countByFollowerId(user.getId());
        
        long recipeCount = 0;
        boolean isFollowing = false;
        
        if (currentUsername != null && currentUsername.equals(user.getUsername())) {
            // Xem profile của chính mình: đếm tất cả công thức (trừ DELETED)
            recipeCount = recipeRepository.countByAuthorIdAndStatusNot(user.getId(), RecipeStatus.DELETED);
        } else {
            // Xem profile người khác: chỉ đếm công thức PUBLIC
            recipeCount = recipeRepository.countByAuthorIdAndStatus(user.getId(), RecipeStatus.PUBLIC);
            
            // Kiểm tra xem currentUsername đã follow user này chưa
            if (currentUsername != null) {
                User currentUser = userRepository.findByUsername(currentUsername).orElse(null);
                if (currentUser != null) {
                    isFollowing = followRepository.existsByFollowerIdAndFollowingId(currentUser.getId(), user.getId());
                }
            }
        }

        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .recipeCount((int) recipeCount)
                .followerCount(followerCount)
                .followingCount(followingCount)
                .isFollowing(isFollowing)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(String username) {
        User user = getUserByUsername(username);
        return mapToResponse(user, username);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getPublicUserProfile(Long userId, String currentUsername) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return mapToResponse(user, currentUsername);
    }

    @Override
    public UserProfileResponse updateProfile(String username, UpdateProfileRequest request) {
        User user = getUserByUsername(username);
        
        if (request.getDisplayName() != null) {
            user.setDisplayName(request.getDisplayName());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }
        
        User updatedUser = userRepository.save(user);
        return mapToResponse(updatedUser, username);
    }

    @Override
    public void changePassword(String username, ChangePasswordRequest request) {
        User user = getUserByUsername(username);
        
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Mật khẩu cũ không chính xác");
        }
        
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    public UserProfileResponse updateAvatar(String username, org.springframework.web.multipart.MultipartFile file) {
        User user = getUserByUsername(username);
        
        String avatarUrl = cloudinaryService.uploadImage(file, "smartrecipe/avatars");
        user.setAvatarUrl(avatarUrl);
        User updatedUser = userRepository.save(user);
        
        return mapToResponse(updatedUser, username);
    }
}

package com.smartrecipe.smartrecipe_backend.service.impl;

import com.smartrecipe.smartrecipe_backend.dto.request.ChangePasswordRequest;
import com.smartrecipe.smartrecipe_backend.dto.request.UpdateProfileRequest;
import com.smartrecipe.smartrecipe_backend.dto.response.UserProfileResponse;
import com.smartrecipe.smartrecipe_backend.entity.GroceryList;
import com.smartrecipe.smartrecipe_backend.entity.User;
import com.smartrecipe.smartrecipe_backend.exception.BadRequestException;
import com.smartrecipe.smartrecipe_backend.exception.ResourceNotFoundException;
import com.smartrecipe.smartrecipe_backend.repository.*;
import com.smartrecipe.smartrecipe_backend.enums.RecipeStatus;
import com.smartrecipe.smartrecipe_backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final RecipeRepository recipeRepository;
    private final AiSuggestionLogRepository aiSuggestionLogRepository;
    private final CookingJournalRepository cookingJournalRepository;
    private final NotificationRepository notificationRepository;
    private final RecipeLikeRepository recipeLikeRepository;
    private final RecipeCommentRepository recipeCommentRepository;
    private final PantryRepository pantryRepository;
    private final GroceryListRepository groceryListRepository;
    private final GroceryListRecipeRepository groceryListRecipeRepository;
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
        
        // Đếm số công thức PUBLIC đã đóng góp cho cộng đồng (áp dụng cho cả trang cá nhân và trang công khai)
        recipeCount = recipeRepository.countByAuthorIdAndStatus(user.getId(), RecipeStatus.PUBLIC);
        long totalLikes = recipeRepository.sumLikeCountByAuthorId(user.getId());
        
        if (currentUsername != null && !currentUsername.equals(user.getUsername())) {
            // Kiểm tra xem currentUsername đã follow user này chưa (khi xem profile người khác)
            User currentUser = userRepository.findByUsername(currentUsername).orElse(null);
            if (currentUser != null) {
                isFollowing = followRepository.existsByFollowerIdAndFollowingId(currentUser.getId(), user.getId());
            }
        }

        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .role(user.getRole() != null ? user.getRole().name() : "USER") // Luôn trả về role
                .recipeCount((int) recipeCount)
                .followerCount(followerCount)
                .followingCount(followingCount)
                .totalLikes(totalLikes)
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

    @Override
    public void deleteAccount(String username, String password) {
        User user = getUserByUsername(username);

        // Xác minh mật khẩu trước khi xóa — tránh xóa nhầm do click nhầm
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BadRequestException("Mật khẩu không chính xác. Vui lòng nhập lại.");
        }

        deleteUserCascade(user.getId());
    }

    @Override
    @Transactional
    public void deleteUserCascade(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // 1. Lấy danh sách ID công thức của user
        List<Long> recipeIds = recipeRepository.findIdsByAuthorId(userId);

        if (recipeIds != null && !recipeIds.isEmpty()) {
            // 2. Gỡ liên kết clonedFrom của các công thức clone từ công thức của user này
            recipeRepository.clearClonedFromByRecipeIds(recipeIds);

            // 3. Gỡ liên kết savedRecipe trong nhật ký AI gợi ý
            aiSuggestionLogRepository.clearSavedRecipeByRecipeIds(recipeIds);

            // 4. Gỡ liên kết recipe trong nhật ký nấu ăn
            cookingJournalRepository.clearRecipeByRecipeIds(recipeIds);

            // 5. Xóa thông báo liên quan đến các công thức này
            notificationRepository.deleteByRecipeIdIn(recipeIds);

            // 5b. Xóa liên kết công thức trong danh sách đi chợ của bất kỳ user nào
            groceryListRecipeRepository.deleteByRecipeIdIn(recipeIds);

            // 5c. Gỡ liên kết parent của các bình luận thuộc các công thức này (tránh self-reference FK error)
            recipeCommentRepository.clearParentByRecipeIds(recipeIds);

            // 6. Xóa các công thức (JPA cascade xóa steps, ingredients, tags, likes, comments của công thức)
            recipeRepository.deleteAllById(recipeIds);
        }

        // 7. Xóa toàn bộ thông báo mà user là người nhận (recipient) hoặc người thực hiện (actor)
        notificationRepository.deleteByRecipientIdOrActorId(userId);

        // 8. Xóa toàn bộ quan hệ follow (follower hoặc following)
        followRepository.deleteByFollowerIdOrFollowingId(userId);

        // 9a. Giảm likeCount của các công thức mà user này đã từng thả tim
        recipeRepository.decrementLikeCountForUserLikes(userId);

        // 9b. Xóa toàn bộ lượt thích của user này
        recipeLikeRepository.deleteByUserId(userId);

        // 10a. Gỡ liên kết parent của các bình luận con trỏ tới bình luận do user này viết
        recipeCommentRepository.clearParentByUserId(userId);

        // 10b. Xóa toàn bộ bình luận do user này viết trên các công thức khác
        recipeCommentRepository.deleteByUserId(userId);

        // 11. Xóa toàn bộ nhật ký nấu ăn của user
        cookingJournalRepository.deleteByUserId(userId);

        // 12. Xóa toàn bộ nhật ký gợi ý AI của user
        aiSuggestionLogRepository.deleteByUserId(userId);

        // 13. Xóa toàn bộ nguyên liệu trong tủ bếp của user
        pantryRepository.deleteByUserId(userId);

        // 14. Xóa toàn bộ danh sách đi chợ của user (JPA cascade xóa items và recipeSources)
        List<GroceryList> userLists = groceryListRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (userLists != null && !userLists.isEmpty()) {
            groceryListRepository.deleteAll(userLists);
        }

        // 15. Cuối cùng, xóa tài khoản người dùng
        userRepository.delete(user);
    }
}

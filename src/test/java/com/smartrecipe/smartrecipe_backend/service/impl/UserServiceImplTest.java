package com.smartrecipe.smartrecipe_backend.service.impl;

import com.smartrecipe.smartrecipe_backend.entity.GroceryList;
import com.smartrecipe.smartrecipe_backend.entity.User;
import com.smartrecipe.smartrecipe_backend.exception.BadRequestException;
import com.smartrecipe.smartrecipe_backend.exception.ResourceNotFoundException;
import com.smartrecipe.smartrecipe_backend.repository.*;
import com.smartrecipe.smartrecipe_backend.service.CloudinaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private FollowRepository followRepository;
    @Mock private RecipeRepository recipeRepository;
    @Mock private AiSuggestionLogRepository aiSuggestionLogRepository;
    @Mock private CookingJournalRepository cookingJournalRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private RecipeLikeRepository recipeLikeRepository;
    @Mock private RecipeCommentRepository recipeCommentRepository;
    @Mock private PantryRepository pantryRepository;
    @Mock private GroceryListRepository groceryListRepository;
    @Mock private GroceryListRecipeRepository groceryListRecipeRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private CloudinaryService cloudinaryService;

    private UserServiceImpl userService;
    private User testUser;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(
                userRepository,
                followRepository,
                recipeRepository,
                aiSuggestionLogRepository,
                cookingJournalRepository,
                notificationRepository,
                recipeLikeRepository,
                recipeCommentRepository,
                pantryRepository,
                groceryListRepository,
                groceryListRecipeRepository,
                passwordEncoder,
                cloudinaryService
        );

        testUser = User.builder()
                .id(42L)
                .username("testuser")
                .passwordHash("hashed_password")
                .email("test@example.com")
                .build();
    }

    @Test
    @DisplayName("deleteUserCascade: User có công thức và dữ liệu liên quan -> Xóa tuần tự toàn bộ theo đúng 15 bước")
    void deleteUserCascade_withRecipesAndRelatedData_shouldDeleteInCorrectOrder() {
        Long userId = 42L;
        List<Long> recipeIds = List.of(101L, 102L);
        GroceryList userList = GroceryList.builder().id(201L).user(testUser).build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(recipeRepository.findIdsByAuthorId(userId)).thenReturn(recipeIds);
        when(groceryListRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(userList));

        userService.deleteUserCascade(userId);

        // Kiểm tra thứ tự thực hiện chính xác bằng InOrder
        InOrder inOrder = inOrder(
                recipeRepository,
                aiSuggestionLogRepository,
                cookingJournalRepository,
                notificationRepository,
                groceryListRecipeRepository,
                recipeCommentRepository,
                followRepository,
                recipeLikeRepository,
                pantryRepository,
                groceryListRepository,
                userRepository
        );

        // Bước 1-6: Xử lý công thức của user
        inOrder.verify(recipeRepository).findIdsByAuthorId(userId);
        inOrder.verify(recipeRepository).clearClonedFromByRecipeIds(recipeIds);
        inOrder.verify(aiSuggestionLogRepository).clearSavedRecipeByRecipeIds(recipeIds);
        inOrder.verify(cookingJournalRepository).clearRecipeByRecipeIds(recipeIds);
        inOrder.verify(notificationRepository).deleteByRecipeIdIn(recipeIds);
        inOrder.verify(groceryListRecipeRepository).deleteByRecipeIdIn(recipeIds);
        inOrder.verify(recipeCommentRepository).clearParentByRecipeIds(recipeIds);
        inOrder.verify(recipeRepository).deleteAllById(recipeIds);

        // Bước 7-14: Xóa dữ liệu trực tiếp của user
        inOrder.verify(notificationRepository).deleteByRecipientIdOrActorId(userId);
        inOrder.verify(followRepository).deleteByFollowerIdOrFollowingId(userId);
        inOrder.verify(recipeRepository).decrementLikeCountForUserLikes(userId);
        inOrder.verify(recipeLikeRepository).deleteByUserId(userId);
        inOrder.verify(recipeCommentRepository).clearParentByUserId(userId);
        inOrder.verify(recipeCommentRepository).deleteByUserId(userId);
        inOrder.verify(cookingJournalRepository).deleteByUserId(userId);
        inOrder.verify(aiSuggestionLogRepository).deleteByUserId(userId);
        inOrder.verify(pantryRepository).deleteByUserId(userId);
        inOrder.verify(groceryListRepository).deleteAll(List.of(userList));

        // Bước 15: Xóa user cuối cùng
        inOrder.verify(userRepository).delete(testUser);
    }

    @Test
    @DisplayName("deleteUserCascade: User không có công thức -> Bỏ qua các bước xử lý theo recipeIds")
    void deleteUserCascade_withoutRecipes_shouldSkipRecipeRelatedSteps() {
        Long userId = 42L;

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(recipeRepository.findIdsByAuthorId(userId)).thenReturn(Collections.emptyList());
        when(groceryListRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(Collections.emptyList());

        userService.deleteUserCascade(userId);

        // Không gọi các thao tác theo recipeIds
        verify(recipeRepository, never()).clearClonedFromByRecipeIds(any());
        verify(aiSuggestionLogRepository, never()).clearSavedRecipeByRecipeIds(any());
        verify(cookingJournalRepository, never()).clearRecipeByRecipeIds(any());
        verify(notificationRepository, never()).deleteByRecipeIdIn(any());
        verify(groceryListRecipeRepository, never()).deleteByRecipeIdIn(any());
        verify(recipeCommentRepository, never()).clearParentByRecipeIds(any());
        verify(recipeRepository, never()).deleteAllById(any());

        // Các bước xóa user trực tiếp vẫn được gọi
        verify(notificationRepository).deleteByRecipientIdOrActorId(userId);
        verify(followRepository).deleteByFollowerIdOrFollowingId(userId);
        verify(recipeRepository).decrementLikeCountForUserLikes(userId);
        verify(recipeLikeRepository).deleteByUserId(userId);
        verify(recipeCommentRepository).clearParentByUserId(userId);
        verify(recipeCommentRepository).deleteByUserId(userId);
        verify(cookingJournalRepository).deleteByUserId(userId);
        verify(aiSuggestionLogRepository).deleteByUserId(userId);
        verify(pantryRepository).deleteByUserId(userId);
        verify(userRepository).delete(testUser);
    }

    @Test
    @DisplayName("deleteUserCascade: User không tồn tại -> Ném ResourceNotFoundException")
    void deleteUserCascade_userNotFound_shouldThrowException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUserCascade(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with id: 999");

        verify(userRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteAccount: Mật khẩu chính xác -> Thực hiện cascade delete user")
    void deleteAccount_correctPassword_shouldCascadeDelete() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("correctPassword", "hashed_password")).thenReturn(true);
        when(userRepository.findById(42L)).thenReturn(Optional.of(testUser));
        when(recipeRepository.findIdsByAuthorId(42L)).thenReturn(Collections.emptyList());
        when(groceryListRepository.findByUserIdOrderByCreatedAtDesc(42L)).thenReturn(Collections.emptyList());

        userService.deleteAccount("testuser", "correctPassword");

        verify(userRepository).delete(testUser);
    }

    @Test
    @DisplayName("deleteAccount: Mật khẩu sai -> Ném BadRequestException và không xóa")
    void deleteAccount_wrongPassword_shouldThrowException() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongPassword", "hashed_password")).thenReturn(false);

        assertThatThrownBy(() -> userService.deleteAccount("testuser", "wrongPassword"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Mật khẩu không chính xác");

        verify(userRepository, never()).delete(any());
    }
}

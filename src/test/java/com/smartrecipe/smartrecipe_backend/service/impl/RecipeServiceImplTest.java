package com.smartrecipe.smartrecipe_backend.service.impl;

import com.smartrecipe.smartrecipe_backend.dto.request.RecipeRequest;
import com.smartrecipe.smartrecipe_backend.dto.response.JournalResponse;
import com.smartrecipe.smartrecipe_backend.dto.response.RecipeResponse;
import com.smartrecipe.smartrecipe_backend.entity.Recipe;
import com.smartrecipe.smartrecipe_backend.entity.RecipeLike;
import com.smartrecipe.smartrecipe_backend.entity.RecipeStep;
import com.smartrecipe.smartrecipe_backend.entity.User;
import com.smartrecipe.smartrecipe_backend.enums.RecipeStatus;
import com.smartrecipe.smartrecipe_backend.exception.UnauthorizedException;
import com.smartrecipe.smartrecipe_backend.repository.*;
import com.smartrecipe.smartrecipe_backend.service.CloudinaryService;
import com.smartrecipe.smartrecipe_backend.service.PantryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecipeServiceImplTest {

    @Mock RecipeRepository recipeRepository;
    @Mock RecipeStepRepository recipeStepRepository;
    @Mock RecipeIngredientRepository recipeIngredientRepository;
    @Mock RecipeTagRepository recipeTagRepository;
    @Mock RecipeLikeRepository recipeLikeRepository;
    @Mock RecipeCommentRepository recipeCommentRepository;
    @Mock UserRepository userRepository;
    @Mock IngredientRepository ingredientRepository;
    @Mock TagRepository tagRepository;
    @Mock CloudinaryService cloudinaryService;
    @Mock CookingJournalRepository cookingJournalRepository;
    @Mock com.smartrecipe.smartrecipe_backend.service.UnitNormalizationService unitNormalizationService;
    @Mock PantryService pantryService;
    @Mock com.smartrecipe.smartrecipe_backend.service.NotificationService notificationService;

    private RecipeServiceImpl service;
    private User owner;
    private User otherUser;

    @BeforeEach
    void setUp() {
        service = new RecipeServiceImpl(
                recipeRepository,
                recipeStepRepository,
                recipeIngredientRepository,
                recipeTagRepository,
                recipeLikeRepository,
                recipeCommentRepository,
                userRepository,
                ingredientRepository,
                tagRepository,
                cloudinaryService,
                cookingJournalRepository,
                unitNormalizationService,
                pantryService,
                notificationService);
        owner = User.builder().id(1L).username("owner").build();
        otherUser = User.builder().id(2L).username("other").build();
    }

    @Test
    void updateRejectsNonOwner() {
        Recipe recipe = recipe(10L, owner, RecipeStatus.PRIVATE);
        when(recipeRepository.findById(10L)).thenReturn(Optional.of(recipe));

        assertThatThrownBy(() -> service.updateRecipe(10L, minimalRequest(), otherUser.getId()))
                .isInstanceOf(UnauthorizedException.class);

        verify(recipeRepository, never()).save(any());
    }

    @Test
    void deleteSoftDeletesOwnedRecipe() {
        Recipe recipe = recipe(10L, owner, RecipeStatus.PUBLIC);
        when(recipeRepository.findById(10L)).thenReturn(Optional.of(recipe));

        service.deleteRecipe(10L, owner.getId());

        assertThat(recipe.getStatus()).isEqualTo(RecipeStatus.DELETED);
        verify(recipeRepository).save(recipe);
    }

    @Test
    void cloneCreatesPrivateIndependentCopyForCurrentUser() {
        Recipe original = recipe(10L, owner, RecipeStatus.PUBLIC);
        original.setTitle("Canh chua");
        original.setSteps(new ArrayList<>(List.of(RecipeStep.builder()
                .id(100L)
                .stepNumber(1)
                .title("Sơ chế cá")
                .instruction("Nấu canh chua")
                .imageUrl("https://example.com/step1.jpg")
                .recipe(original)
                .build())));
        when(recipeRepository.findById(10L)).thenReturn(Optional.of(original));
        when(userRepository.findById(otherUser.getId())).thenReturn(Optional.of(otherUser));
        when(recipeRepository.save(any(Recipe.class))).thenAnswer(invocation -> {
            Recipe saved = invocation.getArgument(0);
            saved.setId(11L);
            return saved;
        });

        RecipeResponse response = service.cloneRecipe(10L, otherUser.getId());

        ArgumentCaptor<Recipe> captor = ArgumentCaptor.forClass(Recipe.class);
        verify(recipeRepository).save(captor.capture());
        Recipe cloned = captor.getValue();
        assertThat(cloned.getStatus()).isEqualTo(RecipeStatus.PRIVATE);
        assertThat(cloned.getAuthor()).isSameAs(otherUser);
        assertThat(cloned.getClonedFrom()).isSameAs(original);
        assertThat(cloned.getSteps()).hasSize(1);
        RecipeStep clonedStep = cloned.getSteps().getFirst();
        assertThat(clonedStep).isNotSameAs(original.getSteps().getFirst());
        assertThat(clonedStep.getTitle()).isEqualTo("Sơ chế cá");
        assertThat(clonedStep.getImageUrl()).isEqualTo("https://example.com/step1.jpg");
        assertThat(clonedStep.getInstruction()).isEqualTo("Nấu canh chua");
        assertThat(response.getClonedFromId()).isEqualTo(10L);

        // Verify notification sent to original author
        verify(notificationService).createNotificationSafe(
                eq(owner),
                eq(otherUser),
                eq(original),
                isNull(),
                eq(com.smartrecipe.smartrecipe_backend.enums.NotificationType.RECIPE_CLONE),
                contains("Canh chua")
        );
    }

    @Test
    void cloneRecipe_whenSelfClone_doesNotSendNotification() {
        Recipe original = recipe(10L, owner, RecipeStatus.PUBLIC);
        original.setTitle("Canh chua");
        when(recipeRepository.findById(10L)).thenReturn(Optional.of(original));
        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(recipeRepository.save(any(Recipe.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.cloneRecipe(10L, owner.getId());

        verify(notificationService, never()).createNotificationSafe(any(), any(), any(), any(), any(), any());
    }

    @Test
    void likeIsIdempotent() {
        Recipe recipe = recipe(10L, owner, RecipeStatus.PUBLIC);
        recipe.setLikeCount(3);
        when(recipeRepository.findById(10L)).thenReturn(Optional.of(recipe));
        when(userRepository.findById(otherUser.getId())).thenReturn(Optional.of(otherUser));
        when(recipeLikeRepository.existsByUserIdAndRecipeId(otherUser.getId(), 10L))
                .thenReturn(false, true);

        service.likeRecipe(10L, otherUser.getId());
        service.likeRecipe(10L, otherUser.getId());

        assertThat(recipe.getLikeCount()).isEqualTo(4);
        verify(recipeLikeRepository, times(1)).save(any(RecipeLike.class));
        verify(recipeRepository, times(1)).save(recipe);
    }

    @Test
    void recordCookSession_deductsIngredients_andPersistsJournal() {
        Recipe recipe = recipe(10L, owner, RecipeStatus.PUBLIC);
        when(recipeRepository.findById(10L)).thenReturn(Optional.of(recipe));
        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));

        JournalResponse.DeductionDetail detail = JournalResponse.DeductionDetail.builder()
                .ingredientName("Trứng gà").deductedAmount(3.0).unit("quả").build();
        when(pantryService.deductIngredientsForRecipe(owner.getId(), 10L, 2))
                .thenReturn(List.of(detail));

        // simulate save returning the same journal with an ID
        com.smartrecipe.smartrecipe_backend.entity.CookingJournal fakeJournal =
                com.smartrecipe.smartrecipe_backend.entity.CookingJournal.builder()
                        .id(99L).recipe(recipe).user(owner).actualServings(2).build();
        when(cookingJournalRepository.save(any())).thenReturn(fakeJournal);

        JournalResponse response = service.recordCookSession(10L, owner.getId(), null);

        verify(pantryService).deductIngredientsForRecipe(owner.getId(), 10L, 2);
        verify(cookingJournalRepository).save(any());
        assertThat(response.getDeductionSummary()).hasSize(1);
        assertThat(response.getDeductionSummary().get(0).getIngredientName()).isEqualTo("Trứng gà");
    }

    @Test
    void unlikeNeverMakesLikeCountNegative() {
        Recipe recipe = recipe(10L, owner, RecipeStatus.PUBLIC);
        recipe.setLikeCount(0);
        when(recipeRepository.findById(10L)).thenReturn(Optional.of(recipe));
        when(recipeLikeRepository.existsByUserIdAndRecipeId(otherUser.getId(), 10L)).thenReturn(true);

        service.unlikeRecipe(10L, otherUser.getId());

        assertThat(recipe.getLikeCount()).isZero();
        verify(recipeLikeRepository).deleteByUserIdAndRecipeId(otherUser.getId(), 10L);
        verify(recipeRepository).save(recipe);
    }

    private RecipeRequest minimalRequest() {
        RecipeRequest request = new RecipeRequest();
        request.setTitle("Món ăn");
        request.setBaseServings(2);
        request.setStatus(RecipeStatus.PRIVATE);
        return request;
    }

    private Recipe recipe(Long id, User author, RecipeStatus status) {
        return Recipe.builder()
                .id(id)
                .title("Recipe")
                .baseServings(2)
                .status(status)
                .likeCount(0)
                .author(author)
                .steps(new ArrayList<>())
                .ingredients(new ArrayList<>())
                .tags(new ArrayList<>())
                .build();
    }
}
package com.smartrecipe.smartrecipe_backend.service.impl;

import com.smartrecipe.smartrecipe_backend.dto.request.RecipeRequest;
import com.smartrecipe.smartrecipe_backend.dto.response.RecipeResponse;
import com.smartrecipe.smartrecipe_backend.entity.Recipe;
import com.smartrecipe.smartrecipe_backend.entity.RecipeLike;
import com.smartrecipe.smartrecipe_backend.entity.RecipeStep;
import com.smartrecipe.smartrecipe_backend.entity.User;
import com.smartrecipe.smartrecipe_backend.enums.RecipeStatus;
import com.smartrecipe.smartrecipe_backend.exception.UnauthorizedException;
import com.smartrecipe.smartrecipe_backend.repository.*;
import com.smartrecipe.smartrecipe_backend.service.CloudinaryService;
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
                cloudinaryService);
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
                .instruction("Nấu")
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
        assertThat(cloned.getSteps().getFirst()).isNotSameAs(original.getSteps().getFirst());
        assertThat(response.getClonedFromId()).isEqualTo(10L);
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
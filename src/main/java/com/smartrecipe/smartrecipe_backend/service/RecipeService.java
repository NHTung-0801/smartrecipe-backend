package com.smartrecipe.smartrecipe_backend.service;

import com.smartrecipe.smartrecipe_backend.dto.request.RecipeRequest;
import com.smartrecipe.smartrecipe_backend.dto.request.RecipeSearchRequest;
import com.smartrecipe.smartrecipe_backend.dto.response.ImageUploadResponse;
import com.smartrecipe.smartrecipe_backend.dto.response.RecipeResponse;
import com.smartrecipe.smartrecipe_backend.dto.response.RecipeSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

public interface RecipeService {

    // CRUD
    RecipeResponse createRecipe(RecipeRequest request, Long userId);

    RecipeResponse getRecipeById(Long id);

    RecipeResponse updateRecipe(Long id, RecipeRequest request, Long userId);

    void deleteRecipe(Long id, Long userId);

    // My Recipes (Draft & Public, exclude DELETED)
    Page<RecipeSummaryResponse> getMyRecipes(Long userId, int page, int size);

    // Public Recipes (Explore)
    Page<RecipeSummaryResponse> getPublicRecipes(int page, int size);

    // Search
    Page<RecipeSummaryResponse> searchRecipes(RecipeSearchRequest request, int page, int size);

    // User Profile
    Page<RecipeSummaryResponse> getUserPublicRecipes(Long userId, int page, int size);

    // Clone
    RecipeResponse cloneRecipe(Long id, Long userId);

    // Like/Unlike
    void likeRecipe(Long recipeId, Long userId);

    void unlikeRecipe(Long recipeId, Long userId);

    // Upload image
    ImageUploadResponse uploadRecipeImage(Long recipeId, MultipartFile file, Long userId);
}

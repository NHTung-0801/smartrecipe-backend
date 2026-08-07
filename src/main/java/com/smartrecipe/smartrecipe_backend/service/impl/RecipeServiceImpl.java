package com.smartrecipe.smartrecipe_backend.service.impl;

import com.smartrecipe.smartrecipe_backend.dto.request.RecipeIngredientRequest;
import com.smartrecipe.smartrecipe_backend.dto.request.RecipeRequest;
import com.smartrecipe.smartrecipe_backend.dto.request.RecipeSearchRequest;
import com.smartrecipe.smartrecipe_backend.dto.request.RecipeStepRequest;
import com.smartrecipe.smartrecipe_backend.dto.response.ImageUploadResponse;
import com.smartrecipe.smartrecipe_backend.dto.response.*;
import com.smartrecipe.smartrecipe_backend.entity.*;
import com.smartrecipe.smartrecipe_backend.enums.RecipeStatus;
import com.smartrecipe.smartrecipe_backend.exception.ResourceNotFoundException;
import com.smartrecipe.smartrecipe_backend.exception.UnauthorizedException;
import com.smartrecipe.smartrecipe_backend.repository.*;
import com.smartrecipe.smartrecipe_backend.service.CloudinaryService;
import com.smartrecipe.smartrecipe_backend.service.RecipeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class RecipeServiceImpl implements RecipeService {

    private final RecipeRepository recipeRepository;
    private final RecipeStepRepository recipeStepRepository;
    private final RecipeIngredientRepository recipeIngredientRepository;
    private final RecipeTagRepository recipeTagRepository;
    private final RecipeLikeRepository recipeLikeRepository;
    private final RecipeCommentRepository recipeCommentRepository;
    private final UserRepository userRepository;
    private final IngredientRepository ingredientRepository;
    private final TagRepository tagRepository;
    private final CloudinaryService cloudinaryService;

    // ==================== CRUD ====================

    @Override
    public RecipeResponse createRecipe(RecipeRequest request, Long userId) {
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId));

        Recipe recipe = Recipe.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .baseServings(request.getBaseServings())
                .status(request.getStatus())
                .imageUrl(request.getImageUrl())
                .prepTime(request.getPrepTime())
                .cookTime(request.getCookTime())
                .difficulty(request.getDifficulty())
                .likeCount(0)
                .author(author)
                .build();

        // Steps
        if (request.getSteps() != null) {
            List<RecipeStep> steps = request.getSteps().stream().map(s -> RecipeStep.builder()
                    .stepNumber(s.getStepNumber())
                    .instruction(s.getInstruction())
                    .recipe(recipe)
                    .build()).collect(Collectors.toList());
            recipe.setSteps(steps);
        }

        // Ingredients
        if (request.getIngredients() != null) {
            List<RecipeIngredient> ingredients = request.getIngredients().stream().map(ri -> {
                Ingredient ing = ingredientRepository.findById(ri.getIngredientId())
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nguyên liệu với ID: " + ri.getIngredientId()));
                return RecipeIngredient.builder()
                        .amount(ri.getAmount())
                        .unit(ri.getUnit())
                        .recipe(recipe)
                        .ingredient(ing)
                        .build();
            }).collect(Collectors.toList());
            recipe.setIngredients(ingredients);
        }

        // Tags
        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            List<RecipeTag> tags = request.getTagIds().stream().map(tagId -> {
                Tag tag = tagRepository.findById(tagId)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thẻ tag với ID: " + tagId));
                return RecipeTag.builder()
                        .recipe(recipe)
                        .tag(tag)
                        .build();
            }).collect(Collectors.toList());
            recipe.setTags(tags);
        }

        Recipe saved = recipeRepository.save(recipe);
        return mapToDetailResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public RecipeResponse getRecipeById(Long id) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công thức với ID: " + id));
        return mapToDetailResponse(recipe);
    }

    @Override
    public RecipeResponse updateRecipe(Long id, RecipeRequest request, Long userId) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công thức với ID: " + id));

        if (!recipe.getAuthor().getId().equals(userId)) {
            throw new UnauthorizedException("Bạn không có quyền sửa công thức này");
        }

        recipe.setTitle(request.getTitle());
        recipe.setDescription(request.getDescription());
        recipe.setBaseServings(request.getBaseServings());
        recipe.setStatus(request.getStatus());
        recipe.setImageUrl(request.getImageUrl());
        recipe.setPrepTime(request.getPrepTime());
        recipe.setCookTime(request.getCookTime());
        recipe.setDifficulty(request.getDifficulty());

        // Replace steps
        recipe.getSteps().clear();
        if (request.getSteps() != null) {
            request.getSteps().forEach(s -> {
                RecipeStep step = RecipeStep.builder()
                        .stepNumber(s.getStepNumber())
                        .instruction(s.getInstruction())
                        .recipe(recipe)
                        .build();
                recipe.getSteps().add(step);
            });
        }

        // Replace ingredients
        recipe.getIngredients().clear();
        if (request.getIngredients() != null) {
            request.getIngredients().forEach(ri -> {
                Ingredient ing = ingredientRepository.findById(ri.getIngredientId())
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nguyên liệu với ID: " + ri.getIngredientId()));
                RecipeIngredient recIng = RecipeIngredient.builder()
                        .amount(ri.getAmount())
                        .unit(ri.getUnit())
                        .recipe(recipe)
                        .ingredient(ing)
                        .build();
                recipe.getIngredients().add(recIng);
            });
        }

        // Replace tags
        recipe.getTags().clear();
        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            request.getTagIds().forEach(tagId -> {
                Tag tag = tagRepository.findById(tagId)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thẻ tag với ID: " + tagId));
                RecipeTag recTag = RecipeTag.builder()
                        .recipe(recipe)
                        .tag(tag)
                        .build();
                recipe.getTags().add(recTag);
            });
        }

        Recipe saved = recipeRepository.save(recipe);
        return mapToDetailResponse(saved);
    }

    @Override
    public void deleteRecipe(Long id, Long userId) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công thức với ID: " + id));

        if (!recipe.getAuthor().getId().equals(userId)) {
            throw new UnauthorizedException("Bạn không có quyền xóa công thức này");
        }

        recipe.setStatus(RecipeStatus.DELETED);
        recipeRepository.save(recipe);
    }

    @Override
    @Transactional
    public RecipeResponse changeStatus(Long id, com.smartrecipe.smartrecipe_backend.enums.RecipeStatus status, Long userId) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công thức với ID: " + id));

        if (!recipe.getAuthor().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền thay đổi trạng thái công thức này");
        }

        recipe.setStatus(status);
        recipe = recipeRepository.save(recipe);
        return mapToDetailResponse(recipe);
    }

    // ==================== LISTING & SEARCH ====================

    @Override
    @Transactional(readOnly = true)
    public Page<RecipeSummaryResponse> getMyRecipes(Long userId, int page, int size) {
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId));
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        return recipeRepository.findByAuthorAndStatusNot(author, RecipeStatus.DELETED, pageable)
                .map(this::mapToSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RecipeSummaryResponse> getPublicRecipes(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return recipeRepository.findByStatus(RecipeStatus.PUBLIC, pageable)
                .map(this::mapToSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RecipeSummaryResponse> searchRecipes(RecipeSearchRequest request, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return recipeRepository.searchPublicRecipes(request.getKeyword(), pageable)
                .map(this::mapToSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RecipeSummaryResponse> getUserPublicRecipes(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return recipeRepository.findPublicByAuthorId(userId, pageable)
                .map(this::mapToSummaryResponse);
    }

    // ==================== CLONE ====================

    @Override
    public RecipeResponse cloneRecipe(Long id, Long userId) {
        Recipe original = recipeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công thức với ID: " + id));

        User cloner = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId));

        Recipe cloned = Recipe.builder()
                .title(original.getTitle() + " (Bản sao)")
                .description(original.getDescription())
                .baseServings(original.getBaseServings())
                .status(RecipeStatus.PRIVATE) // Bản sao mặc định là PRIVATE
                .imageUrl(original.getImageUrl())
                .prepTime(original.getPrepTime())
                .cookTime(original.getCookTime())
                .difficulty(original.getDifficulty())
                .likeCount(0)
                .clonedFrom(original)
                .author(cloner)
                .build();

        // Clone steps
        List<RecipeStep> clonedSteps = original.getSteps().stream()
                .map(s -> RecipeStep.builder()
                        .stepNumber(s.getStepNumber())
                        .instruction(s.getInstruction())
                        .recipe(cloned)
                        .build())
                .collect(Collectors.toList());
        cloned.setSteps(clonedSteps);

        // Clone ingredients
        List<RecipeIngredient> clonedIngredients = original.getIngredients().stream()
                .map(ri -> RecipeIngredient.builder()
                        .amount(ri.getAmount())
                        .unit(ri.getUnit())
                        .recipe(cloned)
                        .ingredient(ri.getIngredient())
                        .build())
                .collect(Collectors.toList());
        cloned.setIngredients(clonedIngredients);

        // Clone tags
        List<RecipeTag> clonedTags = original.getTags().stream()
                .map(rt -> RecipeTag.builder()
                        .recipe(cloned)
                        .tag(rt.getTag())
                        .build())
                .collect(Collectors.toList());
        cloned.setTags(clonedTags);

        Recipe saved = recipeRepository.save(cloned);
        return mapToDetailResponse(saved);
    }

    // ==================== LIKE / UNLIKE ====================

    @Override
    public void likeRecipe(Long recipeId, Long userId) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công thức với ID: " + recipeId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId));

        if (!recipeLikeRepository.existsByUserIdAndRecipeId(userId, recipeId)) {
            RecipeLike like = RecipeLike.builder()
                    .user(user)
                    .recipe(recipe)
                    .build();
            recipeLikeRepository.save(like);
            recipe.setLikeCount(recipe.getLikeCount() + 1);
            recipeRepository.save(recipe);
        }
    }

    @Override
    public void unlikeRecipe(Long recipeId, Long userId) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công thức với ID: " + recipeId));

        if (recipeLikeRepository.existsByUserIdAndRecipeId(userId, recipeId)) {
            recipeLikeRepository.deleteByUserIdAndRecipeId(userId, recipeId);
            recipe.setLikeCount(Math.max(0, recipe.getLikeCount() - 1));
            recipeRepository.save(recipe);
        }
    }

    // ==================== UPLOAD IMAGE ====================

    @Override
    public ImageUploadResponse uploadRecipeImage(Long recipeId, MultipartFile file, Long userId) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công thức với ID: " + recipeId));

        if (!recipe.getAuthor().getId().equals(userId)) {
            throw new UnauthorizedException("Bạn không có quyền upload ảnh cho công thức này");
        }

        String imageUrl = cloudinaryService.uploadImage(file, "smartrecipe/recipes");
        recipe.setImageUrl(imageUrl);
        recipeRepository.save(recipe);

        return ImageUploadResponse.builder()
                .imageUrl(imageUrl)
                .publicId(imageUrl.substring(imageUrl.lastIndexOf('/') + 1))
                .build();
    }

    // ==================== MAPPING ====================

    private RecipeResponse mapToDetailResponse(Recipe recipe) {
        List<RecipeStepResponse> stepResponses = recipe.getSteps().stream()
                .map(s -> RecipeStepResponse.builder()
                        .id(s.getId())
                        .stepNumber(s.getStepNumber())
                        .instruction(s.getInstruction())
                        .build())
                .collect(Collectors.toList());

        List<RecipeIngredientResponse> ingredientResponses = recipe.getIngredients().stream()
                .map(ri -> RecipeIngredientResponse.builder()
                        .id(ri.getId())
                        .ingredientId(ri.getIngredient().getId())
                        .ingredientName(ri.getIngredient().getName())
                        .amount(ri.getAmount())
                        .unit(ri.getUnit())
                        .aisleId(ri.getIngredient().getAisle() != null ? ri.getIngredient().getAisle().getId().longValue() : null)
                        .aisleName(ri.getIngredient().getAisle() != null ? ri.getIngredient().getAisle().getName() : null)
                        .build())
                .collect(Collectors.toList());

        List<TagResponse> tagResponses = recipe.getTags().stream()
                .map(rt -> TagResponse.builder()
                        .id(rt.getTag().getId())
                        .name(rt.getTag().getName())
                        .build())
                .collect(Collectors.toList());

        NutritionSummaryResponse nutrition = calculateNutrition(recipe);

        return RecipeResponse.builder()
                .id(recipe.getId())
                .title(recipe.getTitle())
                .description(recipe.getDescription())
                .baseServings(recipe.getBaseServings())
                .status(recipe.getStatus())
                .imageUrl(recipe.getImageUrl())
                .prepTime(recipe.getPrepTime())
                .cookTime(recipe.getCookTime())
                .difficulty(recipe.getDifficulty())
                .likeCount(recipe.getLikeCount())
                .clonedFromId(recipe.getClonedFrom() != null ? recipe.getClonedFrom().getId() : null)
                .createdAt(recipe.getCreatedAt())
                .updatedAt(recipe.getUpdatedAt())
                .author(mapAuthor(recipe.getAuthor()))
                .steps(stepResponses)
                .ingredients(ingredientResponses)
                .tags(tagResponses)
                .nutrition(nutrition)
                .build();
    }

    private RecipeSummaryResponse mapToSummaryResponse(Recipe recipe) {
        List<TagResponse> tagResponses = recipe.getTags().stream()
                .map(rt -> TagResponse.builder()
                        .id(rt.getTag().getId())
                        .name(rt.getTag().getName())
                        .build())
                .collect(Collectors.toList());

        NutritionSummaryResponse nutrition = calculateNutrition(recipe);

        return RecipeSummaryResponse.builder()
                .id(recipe.getId())
                .title(recipe.getTitle())
                .description(recipe.getDescription())
                .baseServings(recipe.getBaseServings())
                .status(recipe.getStatus())
                .imageUrl(recipe.getImageUrl())
                .prepTime(recipe.getPrepTime())
                .cookTime(recipe.getCookTime())
                .difficulty(recipe.getDifficulty())
                .likeCount(recipe.getLikeCount())
                .clonedFromId(recipe.getClonedFrom() != null ? recipe.getClonedFrom().getId() : null)
                .createdAt(recipe.getCreatedAt())
                .updatedAt(recipe.getUpdatedAt())
                .author(mapAuthor(recipe.getAuthor()))
                .tags(tagResponses)
                .nutrition(nutrition)
                .build();
    }

    private AuthorSummaryResponse mapAuthor(User author) {
        if (author == null) return null;
        return AuthorSummaryResponse.builder()
                .id(author.getId())
                .username(author.getUsername())
                .displayName(author.getDisplayName())
                .avatarUrl(author.getAvatarUrl())
                .build();
    }

    private NutritionSummaryResponse calculateNutrition(Recipe recipe) {
        BigDecimal totalCalories = BigDecimal.ZERO;
        BigDecimal totalProtein = BigDecimal.ZERO;
        BigDecimal totalFat = BigDecimal.ZERO;
        BigDecimal totalCarbs = BigDecimal.ZERO;

        for (RecipeIngredient ri : recipe.getIngredients()) {
            Ingredient ing = ri.getIngredient();
            if (ing == null) continue;

            // Quy đổi amount về gram nếu cần (đơn giản: giả sử input là grams)
            BigDecimal amountInGrams = ri.getAmount();
            if (!"g".equalsIgnoreCase(ri.getUnit()) && !"gram".equalsIgnoreCase(ri.getUnit())) {
                // Mặc định coi 1 unit = 100g nếu không rõ (có thể mở rộng dùng UnitConversion sau)
                amountInGrams = ri.getAmount().multiply(new BigDecimal("100"));
            }

            BigDecimal ratio = amountInGrams.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);

            totalCalories = totalCalories.add(ing.getCaloriesPer100g().multiply(ratio));
            totalProtein = totalProtein.add(ing.getProtein().multiply(ratio));
            totalFat = totalFat.add(ing.getFat().multiply(ratio));
            totalCarbs = totalCarbs.add(ing.getCarbs().multiply(ratio));
        }

        int servings = recipe.getBaseServings() != null && recipe.getBaseServings() > 0 ? recipe.getBaseServings() : 1;
        BigDecimal svg = new BigDecimal(servings);

        return NutritionSummaryResponse.builder()
                .totalCalories(totalCalories.setScale(2, RoundingMode.HALF_UP))
                .totalProtein(totalProtein.setScale(2, RoundingMode.HALF_UP))
                .totalFat(totalFat.setScale(2, RoundingMode.HALF_UP))
                .totalCarbs(totalCarbs.setScale(2, RoundingMode.HALF_UP))
                .caloriesPerServing(totalCalories.divide(svg, 2, RoundingMode.HALF_UP))
                .proteinPerServing(totalProtein.divide(svg, 2, RoundingMode.HALF_UP))
                .fatPerServing(totalFat.divide(svg, 2, RoundingMode.HALF_UP))
                .carbsPerServing(totalCarbs.divide(svg, 2, RoundingMode.HALF_UP))
                .build();
    }
}
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
import com.smartrecipe.smartrecipe_backend.repository.UserRepository;
import com.smartrecipe.smartrecipe_backend.repository.CookingJournalRepository;
import com.smartrecipe.smartrecipe_backend.enums.NotificationType;
import com.smartrecipe.smartrecipe_backend.service.CloudinaryService;
import com.smartrecipe.smartrecipe_backend.service.NotificationService;
import com.smartrecipe.smartrecipe_backend.service.PantryService;
import com.smartrecipe.smartrecipe_backend.service.RecipeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
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
    private final CookingJournalRepository cookingJournalRepository;
    private final com.smartrecipe.smartrecipe_backend.service.UnitNormalizationService unitNormalizationService;
    private final PantryService pantryService;
    private final NotificationService notificationService;

    // ==================== CRUD ====================

    @Override
    public RecipeResponse createRecipe(RecipeRequest request, Long userId) {
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId));

        // Nếu user gửi PUBLIC → chuyển sang PENDING_REVIEW (chờ Admin duyệt)
        RecipeStatus finalStatus = request.getStatus() == RecipeStatus.PUBLIC
                ? RecipeStatus.PENDING_REVIEW
                : request.getStatus();

        Recipe recipe = Recipe.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .baseServings(request.getBaseServings())
                .status(finalStatus)
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
                    .title(s.getTitle())
                    .instruction(s.getInstruction())
                    .recipe(recipe)
                    .build()).collect(Collectors.toList());
            recipe.setSteps(steps);
        }

        // Ingredients
        if (request.getIngredients() != null) {
            List<RecipeIngredient> ingredients = request.getIngredients().stream().map(ri -> {
                Ingredient ing;
                if (ri.getIngredientId() != null) {
                    ing = ingredientRepository.findById(ri.getIngredientId())
                            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nguyên liệu với ID: " + ri.getIngredientId()));
                } else if (ri.getIngredientName() != null && !ri.getIngredientName().trim().isEmpty()) {
                    String name = ri.getIngredientName().trim();
                    ing = ingredientRepository.findFirstByNameIgnoreCase(name)
                            .orElseGet(() -> {
                                Ingredient newIng = new Ingredient();
                                newIng.setName(name);
                                newIng.setBaseUnit(ri.getUnit() != null && !ri.getUnit().isEmpty() ? ri.getUnit() : "g");
                                newIng.setCaloriesPer100g(java.math.BigDecimal.ZERO);
                                newIng.setProtein(java.math.BigDecimal.ZERO);
                                newIng.setFat(java.math.BigDecimal.ZERO);
                                newIng.setCarbs(java.math.BigDecimal.ZERO);
                                return ingredientRepository.save(newIng);
                            });
                } else {
                    throw new IllegalArgumentException("Phải cung cấp ID hoặc tên nguyên liệu");
                }
                
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
        // Nếu user muốn chuyển sang PUBLIC → PENDING_REVIEW để admin duyệt
        // Chỉ ADMIN (qua AdminController) mới được set trực tiếp PUBLIC
        RecipeStatus updateStatus = request.getStatus() == RecipeStatus.PUBLIC
                ? RecipeStatus.PENDING_REVIEW
                : request.getStatus();
        recipe.setStatus(updateStatus);
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
                        .title(s.getTitle())
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
                Ingredient ing;
                if (ri.getIngredientId() != null) {
                    ing = ingredientRepository.findById(ri.getIngredientId())
                            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nguyên liệu với ID: " + ri.getIngredientId()));
                } else if (ri.getIngredientName() != null && !ri.getIngredientName().trim().isEmpty()) {
                    String name = ri.getIngredientName().trim();
                    ing = ingredientRepository.findFirstByNameIgnoreCase(name)
                            .orElseGet(() -> {
                                Ingredient newIng = new Ingredient();
                                newIng.setName(name);
                                newIng.setBaseUnit(ri.getUnit() != null && !ri.getUnit().isEmpty() ? ri.getUnit() : "g");
                                newIng.setCaloriesPer100g(java.math.BigDecimal.ZERO);
                                newIng.setProtein(java.math.BigDecimal.ZERO);
                                newIng.setFat(java.math.BigDecimal.ZERO);
                                newIng.setCarbs(java.math.BigDecimal.ZERO);
                                return ingredientRepository.save(newIng);
                            });
                } else {
                    throw new IllegalArgumentException("Phải cung cấp ID hoặc tên nguyên liệu");
                }

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
        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            java.util.Set<Integer> newTagIds = new java.util.HashSet<>(request.getTagIds());
            
            // Remove tags not in the new list
            recipe.getTags().removeIf(rt -> !newTagIds.contains(rt.getTag().getId()));
            
            // Find existing tag IDs
            List<Integer> existingTagIds = recipe.getTags().stream()
                    .map(rt -> rt.getTag().getId()).collect(java.util.stream.Collectors.toList());
            
            // Add new tags
            newTagIds.forEach(tagId -> {
                if (!existingTagIds.contains(tagId)) {
                    Tag tag = tagRepository.findById(tagId)
                            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thẻ tag với ID: " + tagId));
                    RecipeTag recTag = RecipeTag.builder()
                            .recipe(recipe)
                            .tag(tag)
                            .build();
                    recipe.getTags().add(recTag);
                }
            });
        } else {
            recipe.getTags().clear();
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

        // Nếu user muốn đăng PUBLIC → chuyển sang PENDING_REVIEW để Admin kiểm duyệt
        // Chỉ Admin (qua AdminController) mới được set trực tiếp PUBLIC
        RecipeStatus finalStatus = (status == RecipeStatus.PUBLIC)
                ? RecipeStatus.PENDING_REVIEW
                : status;

        recipe.setStatus(finalStatus);
        recipe = recipeRepository.save(recipe);
        return mapToDetailResponse(recipe);
    }

    // ==================== LISTING & SEARCH ====================

    @Override
    @Transactional(readOnly = true)
    public Page<RecipeSummaryResponse> getMyRecipes(Long userId, int page, int size) {
        return getMyRecipes(userId, null, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RecipeSummaryResponse> getMyRecipes(Long userId, String status, int page, int size) {
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId));
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));

        Page<Recipe> pageResult;
        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status.trim())) {
            String normalizedStatus = status.trim().toUpperCase();
            // Tab "Riêng tư / Nháp" (PRIVATE) bao gồm cả PRIVATE lẫn DRAFT
            if ("PRIVATE".equals(normalizedStatus)) {
                List<RecipeStatus> privateAndDraft = Arrays.asList(RecipeStatus.PRIVATE, RecipeStatus.DRAFT);
                pageResult = recipeRepository.findByAuthorAndStatusIn(author, privateAndDraft, pageable);
            } else {
                RecipeStatus recipeStatus = RecipeStatus.valueOf(normalizedStatus);
                pageResult = recipeRepository.findByAuthorAndStatus(author, recipeStatus, pageable);
            }
        } else {
            pageResult = recipeRepository.findByAuthorAndStatusNot(author, RecipeStatus.DELETED, pageable);
        }

        return pageResult.map(this::mapToSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RecipeSummaryResponse> getPublicRecipes(int page, int size, String sortBy) {
        Sort sort = "likeCount".equalsIgnoreCase(sortBy)
                ? Sort.by(Sort.Direction.DESC, "likeCount").and(Sort.by(Sort.Direction.DESC, "createdAt"))
                : Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable pageable = PageRequest.of(page, size, sort);
        return recipeRepository.findByStatus(RecipeStatus.PUBLIC, pageable)
                .map(this::mapToSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RecipeSummaryResponse> getPublicRecipes(int page, int size) {
        return getPublicRecipes(page, size, "createdAt");
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
    @Transactional
    public JournalResponse recordCookSession(Long id, Long userId, Integer servings) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công thức với ID: " + id));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId));

        // Xác định số khẩu phần thực tế
        int actualServings = (servings != null && servings > 0) ? servings : recipe.getBaseServings();

        // Bước 1 — Tự động trừ kho nguyên liệu theo thuật toán FEFO
        List<JournalResponse.DeductionDetail> deductions;
        try {
            deductions = pantryService.deductIngredientsForRecipe(userId, recipe.getId(), actualServings);
            log.info("[recordCookSession] Đã trừ {} nguyên liệu khỏi tủ cho user={}, recipe={}",
                    deductions.size(), userId, id);
        } catch (Exception e) {
            // Nếu việc trừ kho thất bại, ghi log nhưng không chặn việc lưu nhật ký
            log.warn("[recordCookSession] Trừ kho thất bại cho user={}, recipe={}: {}", userId, id, e.getMessage());
            deductions = java.util.List.of();
        }

        // Bước 2 — Lưu nhật ký nấu ăn
        CookingJournal journal = CookingJournal.builder()
                .recipe(recipe)
                .user(user)
                .actualServings(actualServings)
                .rating(null) // Rating sẽ được cập nhật sau qua nhật ký
                .build();
        CookingJournal saved = cookingJournalRepository.save(journal);

        // Bước 3 — Đóng gói kết quả trả về
        JournalResponse.RecipeSummaryInfo recipeInfo = JournalResponse.RecipeSummaryInfo.builder()
                .id(recipe.getId())
                .title(recipe.getTitle())
                .imageUrl(recipe.getImageUrl())
                .baseServings(recipe.getBaseServings())
                .build();

        JournalResponse response = JournalResponse.builder()
                .id(saved.getId())
                .recipe(recipeInfo)
                .cookedAt(saved.getCookedAt())
                .actualServings(saved.getActualServings())
                .rating(null)
                .deductionSummary(deductions)
                .build();

        return response;
    }

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
                        .title(s.getTitle())
                        .instruction(s.getInstruction())
                        .imageUrl(s.getImageUrl())
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

        // Gửi thông báo cho tác giả công thức gốc nếu người clone không phải chính tác giả
        if (original.getAuthor() != null && !original.getAuthor().getId().equals(cloner.getId())) {
            String clonerName = cloner.getDisplayName() != null && !cloner.getDisplayName().trim().isEmpty()
                    ? cloner.getDisplayName()
                    : cloner.getUsername();
            notificationService.createNotificationSafe(
                    original.getAuthor(),
                    cloner,
                    original,
                    null,
                    NotificationType.RECIPE_CLONE,
                    clonerName + " đã sao chép công thức \"" + original.getTitle() + "\" của bạn"
            );
        }

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

            // Thông báo cho tác giả khi có người thích công thức của họ
            String likerName = user.getDisplayName() != null ? user.getDisplayName() : user.getUsername();
            notificationService.createNotificationSafe(
                    recipe.getAuthor(),
                    user,
                    recipe,
                    null,
                    NotificationType.RECIPE_LIKE,
                    likerName + " đã thích công thức \"" + recipe.getTitle() + "\" của bạn"
            );
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

    @Override
    @Transactional(readOnly = true)
    public List<Long> getLikedRecipeIds(Long userId) {
        return recipeLikeRepository.findLikedRecipeIdsByUserId(userId);
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

    @Override
    public ImageUploadResponse uploadStepImage(Long recipeId, Integer stepNumber, MultipartFile file, Long userId) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công thức với ID: " + recipeId));

        if (!recipe.getAuthor().getId().equals(userId)) {
            throw new UnauthorizedException("Bạn không có quyền upload ảnh cho công thức này");
        }

        RecipeStep step = recipe.getSteps().stream()
                .filter(s -> s.getStepNumber().equals(stepNumber))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bước " + stepNumber));

        String imageUrl = cloudinaryService.uploadImage(file, "smartrecipe/recipes/steps");
        step.setImageUrl(imageUrl);
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
                        .title(s.getTitle())
                        .instruction(s.getInstruction())
                        .imageUrl(s.getImageUrl())
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
        Integer cookCount = cookingJournalRepository.countByRecipeId(recipe.getId());
        Integer cloneCount = recipeRepository.countByClonedFromId(recipe.getId());

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
                .cloneCount(cloneCount)
                .cookCount(cookCount)
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
        Integer cookCount = cookingJournalRepository.countByRecipeId(recipe.getId());
        Integer cloneCount = recipeRepository.countByClonedFromId(recipe.getId());

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
                .cloneCount(cloneCount)
                .cookCount(cookCount)
                .createdAt(recipe.getCreatedAt())
                .updatedAt(recipe.getUpdatedAt())
                .author(mapAuthor(recipe.getAuthor()))
                .tags(tagResponses)
                .nutrition(nutrition)
                .ingredientCount(recipe.getIngredients() != null ? recipe.getIngredients().size() : 0)
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

        if (recipe.getIngredients() != null) {
            for (RecipeIngredient ri : recipe.getIngredients()) {
                Ingredient ing = ri.getIngredient();
                if (ing == null || ri.getAmount() == null) continue;

                // Quy đổi amount về gram
                BigDecimal amountInGrams = ri.getAmount();
                if (ri.getUnit() != null && !"g".equalsIgnoreCase(ri.getUnit()) && !"gram".equalsIgnoreCase(ri.getUnit())) {
                    if (unitNormalizationService != null) {
                        try {
                            amountInGrams = unitNormalizationService.toBaseUnit(ri.getAmount(), ri.getUnit(), ing);
                        } catch (Exception e) {
                            // Mặc định coi 1 unit = 100g nếu không có quy đổi cụ thể
                            amountInGrams = ri.getAmount().multiply(new BigDecimal("100"));
                        }
                    } else {
                        amountInGrams = ri.getAmount().multiply(new BigDecimal("100"));
                    }
                }

                BigDecimal ratio = amountInGrams.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);

                BigDecimal cal = ing.getCaloriesPer100g() != null ? ing.getCaloriesPer100g() : BigDecimal.ZERO;
                BigDecimal pro = ing.getProtein() != null ? ing.getProtein() : BigDecimal.ZERO;
                BigDecimal fat = ing.getFat() != null ? ing.getFat() : BigDecimal.ZERO;
                BigDecimal carb = ing.getCarbs() != null ? ing.getCarbs() : BigDecimal.ZERO;

                totalCalories = totalCalories.add(cal.multiply(ratio));
                totalProtein = totalProtein.add(pro.multiply(ratio));
                totalFat = totalFat.add(fat.multiply(ratio));
                totalCarbs = totalCarbs.add(carb.multiply(ratio));
            }
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
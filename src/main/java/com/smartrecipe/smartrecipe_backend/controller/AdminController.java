package com.smartrecipe.smartrecipe_backend.controller;

import com.smartrecipe.smartrecipe_backend.dto.response.ApiResponse;
import com.smartrecipe.smartrecipe_backend.entity.Ingredient;
import com.smartrecipe.smartrecipe_backend.entity.Recipe;
import com.smartrecipe.smartrecipe_backend.enums.GroceryListStatus;
import com.smartrecipe.smartrecipe_backend.enums.RecipeStatus;
import com.smartrecipe.smartrecipe_backend.exception.ResourceNotFoundException;
import com.smartrecipe.smartrecipe_backend.repository.GroceryListRepository;
import com.smartrecipe.smartrecipe_backend.repository.IngredientRepository;
import com.smartrecipe.smartrecipe_backend.repository.RecipeRepository;
import com.smartrecipe.smartrecipe_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final RecipeRepository recipeRepository;
    private final IngredientRepository ingredientRepository;
    private final GroceryListRepository groceryListRepository;

    // ==================== DASHBOARD STATS ====================

    /**
     * KPI tổng quan cho Admin Dashboard
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        stats.put("totalUsers",       userRepository.count());
        stats.put("publicRecipes",     recipeRepository.countByStatus(RecipeStatus.PUBLIC));
        stats.put("totalRecipes",      recipeRepository.countByStatusNot(RecipeStatus.DELETED));
        stats.put("pendingRecipes",    recipeRepository.countByStatus(RecipeStatus.PENDING_REVIEW));
        stats.put("pendingIngredients",ingredientRepository.countPendingReview());
        stats.put("totalIngredients",  ingredientRepository.count());
        stats.put("activeGroceryLists",groceryListRepository.countByStatus(GroceryListStatus.ACTIVE));

        return ResponseEntity.ok(ApiResponse.success(stats, "Thống kê thành công"));
    }

    // ==================== RECIPE MODERATION ====================

    /**
     * Danh sách công thức lọc theo status (mặc định PENDING_REVIEW)
     */
    @GetMapping("/recipes")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAdminRecipes(
            @RequestParam(defaultValue = "PENDING_REVIEW") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        RecipeStatus recipeStatus = RecipeStatus.valueOf(status);
        Pageable pageable = PageRequest.of(page, size);
        Page<Recipe> result = recipeRepository.findByStatusOrderByCreatedAtDesc(recipeStatus, pageable);

        List<Map<String, Object>> items = result.getContent().stream().map(r -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", r.getId());
            item.put("title", r.getTitle());
            item.put("imageUrl", r.getImageUrl());
            item.put("status", r.getStatus().name());
            item.put("difficulty", r.getDifficulty() != null ? r.getDifficulty().name() : null);
            item.put("likeCount", r.getLikeCount());
            item.put("authorId", r.getAuthor().getId());
            item.put("authorUsername", r.getAuthor().getUsername());
            item.put("authorDisplayName", r.getAuthor().getDisplayName());
            item.put("createdAt", r.getCreatedAt());
            String desc = r.getDescription();
            item.put("description", desc != null && desc.length() > 150 ? desc.substring(0, 150) + "..." : desc);
            item.put("tags", r.getTags().stream().map(t -> t.getTag().getName()).toList());
            return item;
        }).toList();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("content", items);
        response.put("totalElements", result.getTotalElements());
        response.put("totalPages", result.getTotalPages());
        response.put("currentPage", page);

        return ResponseEntity.ok(ApiResponse.success(response, "Lấy danh sách công thức thành công"));
    }

    /**
     * Chi tiết công thức (admin preview)
     */
    @GetMapping("/recipes/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAdminRecipeDetail(@PathVariable Long id) {
        Recipe r = recipeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công thức ID: " + id));

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("id",              r.getId());
        detail.put("title",           r.getTitle());
        detail.put("description",     r.getDescription());
        detail.put("imageUrl",        r.getImageUrl());
        detail.put("status",          r.getStatus().name());
        detail.put("difficulty",      r.getDifficulty() != null ? r.getDifficulty().name() : null);
        detail.put("prepTime",        r.getPrepTime());
        detail.put("cookTime",        r.getCookTime());
        detail.put("baseServings",    r.getBaseServings());
        detail.put("likeCount",       r.getLikeCount());
        detail.put("authorUsername",  r.getAuthor().getUsername());
        detail.put("authorDisplayName", r.getAuthor().getDisplayName());
        detail.put("authorAvatar",    r.getAuthor().getAvatarUrl());
        detail.put("createdAt",       r.getCreatedAt());
        detail.put("tags",            r.getTags().stream().map(t -> t.getTag().getName()).toList());
        detail.put("steps",           r.getSteps().stream().map(s -> {
            Map<String, Object> step = new LinkedHashMap<>();
            step.put("stepNumber",  s.getStepNumber());
            step.put("title",       s.getTitle());
            step.put("instruction", s.getInstruction());
            return step;
        }).toList());
        detail.put("ingredients",     r.getIngredients().stream().map(i -> {
            Map<String, Object> ing = new LinkedHashMap<>();
            ing.put("name",   i.getIngredient().getName());
            ing.put("amount", i.getAmount());
            ing.put("unit",   i.getUnit());
            return ing;
        }).toList());

        return ResponseEntity.ok(ApiResponse.success(detail, "Lấy chi tiết công thức thành công"));
    }

    /**
     * Đổi trạng thái công thức
     * action: APPROVE → PUBLIC | HIDE → PRIVATE | DELETE → DELETED
     */
    @PatchMapping("/recipes/{id}/status")
    public ResponseEntity<ApiResponse<Void>> updateRecipeStatus(
            @PathVariable Long id,
            @RequestParam String action) {

        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công thức ID: " + id));

        RecipeStatus newStatus = switch (action.toUpperCase()) {
            case "APPROVE" -> RecipeStatus.PUBLIC;
            case "HIDE"    -> RecipeStatus.PRIVATE;
            case "DELETE"  -> RecipeStatus.DELETED;
            default -> throw new IllegalArgumentException("Action không hợp lệ: " + action);
        };

        recipe.setStatus(newStatus);
        recipeRepository.save(recipe);

        String message = switch (action.toUpperCase()) {
            case "APPROVE" -> "Đã duyệt và đăng công khai công thức";
            case "HIDE"    -> "Đã ẩn công thức";
            case "DELETE"  -> "Đã xóa công thức";
            default        -> "Cập nhật thành công";
        };

        return ResponseEntity.ok(ApiResponse.success(null, message));
    }

    // ==================== INGREDIENTS ====================

    /**
     * Nguyên liệu chờ duyệt (calories=0, loại trừ muối/gia vị cơ bản)
     */
    @GetMapping("/ingredients/pending-review")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPendingIngredients(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<Ingredient> result = ingredientRepository.findPendingReview(pageable);

        List<Map<String, Object>> items = result.getContent().stream().map(ing -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id",            ing.getId());
            item.put("name",          ing.getName());
            item.put("baseUnit",      ing.getBaseUnit());
            item.put("caloriesPer100g", ing.getCaloriesPer100g());
            item.put("protein",       ing.getProtein());
            item.put("fat",           ing.getFat());
            item.put("carbs",         ing.getCarbs());
            item.put("aisleName",     ing.getAisle() != null ? ing.getAisle().getName() : null);
            item.put("aisleId",       ing.getAisle() != null ? ing.getAisle().getId() : null);
            return item;
        }).toList();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("content", items);
        response.put("totalElements", result.getTotalElements());
        response.put("totalPages", result.getTotalPages());
        response.put("currentPage", page);

        return ResponseEntity.ok(ApiResponse.success(response, "Lấy danh sách nguyên liệu chờ duyệt thành công"));
    }

    /**
     * Tất cả nguyên liệu (phân trang, có tìm kiếm)
     */
    @GetMapping("/ingredients")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllIngredients(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String keyword) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        Page<Ingredient> result = keyword != null && !keyword.isBlank()
                ? ingredientRepository.findByNameContainingIgnoreCase(keyword, pageable)
                : ingredientRepository.findAll(pageable);

        List<Map<String, Object>> items = result.getContent().stream().map(ing -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id",            ing.getId());
            item.put("name",          ing.getName());
            item.put("baseUnit",      ing.getBaseUnit());
            item.put("caloriesPer100g", ing.getCaloriesPer100g());
            item.put("protein",       ing.getProtein());
            item.put("fat",           ing.getFat());
            item.put("carbs",         ing.getCarbs());
            item.put("aisleName",     ing.getAisle() != null ? ing.getAisle().getName() : "—");
            item.put("aisleId",       ing.getAisle() != null ? ing.getAisle().getId() : null);
            return item;
        }).toList();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("content", items);
        response.put("totalElements", result.getTotalElements());
        response.put("totalPages", result.getTotalPages());
        response.put("currentPage", page);

        return ResponseEntity.ok(ApiResponse.success(response, "Lấy danh sách nguyên liệu thành công"));
    }

    /**
     * Cập nhật dinh dưỡng nguyên liệu
     */
    @PatchMapping("/ingredients/{id}")
    public ResponseEntity<ApiResponse<Void>> updateIngredient(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {

        Ingredient ing = ingredientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nguyên liệu ID: " + id));

        if (body.containsKey("caloriesPer100g"))
            ing.setCaloriesPer100g(new java.math.BigDecimal(body.get("caloriesPer100g").toString()));
        if (body.containsKey("protein"))
            ing.setProtein(new java.math.BigDecimal(body.get("protein").toString()));
        if (body.containsKey("fat"))
            ing.setFat(new java.math.BigDecimal(body.get("fat").toString()));
        if (body.containsKey("carbs"))
            ing.setCarbs(new java.math.BigDecimal(body.get("carbs").toString()));

        ingredientRepository.save(ing);
        return ResponseEntity.ok(ApiResponse.success(null, "Cập nhật nguyên liệu thành công"));
    }

    /**
     * Xóa nguyên liệu
     */
    @DeleteMapping("/ingredients/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteIngredient(@PathVariable Long id) {
        if (!ingredientRepository.existsById(id))
            throw new ResourceNotFoundException("Không tìm thấy nguyên liệu ID: " + id);
        ingredientRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã xóa nguyên liệu"));
    }
}

package com.smartrecipe.smartrecipe_backend.controller;

import com.smartrecipe.smartrecipe_backend.dto.response.ApiResponse;
import com.smartrecipe.smartrecipe_backend.entity.Ingredient;
import com.smartrecipe.smartrecipe_backend.entity.Recipe;
import com.smartrecipe.smartrecipe_backend.enums.Difficulty;
import com.smartrecipe.smartrecipe_backend.enums.GroceryListStatus;
import com.smartrecipe.smartrecipe_backend.enums.RecipeStatus;
import com.smartrecipe.smartrecipe_backend.exception.ResourceNotFoundException;
import com.smartrecipe.smartrecipe_backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private final AiSuggestionLogRepository aiSuggestionLogRepository;
    private final PantryRepository pantryRepository;
    private final CookingJournalRepository cookingJournalRepository;
    private final AisleRepository aisleRepository;

    // ==================== DASHBOARD STATS ====================

    /**
     * KPI tổng quan và thống kê thực tế cho Admin Dashboard (Option 2)
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        // ── 1. Nền tảng (Platform KPIs) ──
        stats.put("totalUsers",        userRepository.count());
        stats.put("publicRecipes",      recipeRepository.countByStatus(RecipeStatus.PUBLIC));
        stats.put("totalRecipes",       recipeRepository.countByStatusNot(RecipeStatus.DELETED));
        stats.put("pendingRecipes",     recipeRepository.countByStatus(RecipeStatus.PENDING_REVIEW));
        stats.put("pendingIngredients", ingredientRepository.countPendingReview());
        stats.put("totalIngredients",   ingredientRepository.count());
        stats.put("activeGroceryLists", groceryListRepository.countByStatus(GroceryListStatus.ACTIVE));

        // ── 2. Phân bổ Độ khó (Recipe Difficulty Breakdown) ──
        Map<String, Long> difficultyStats = new LinkedHashMap<>();
        difficultyStats.put("EASY", 0L);
        difficultyStats.put("MEDIUM", 0L);
        difficultyStats.put("HARD", 0L);
        List<Object[]> diffCounts = recipeRepository.countGroupByDifficulty();
        for (Object[] row : diffCounts) {
            if (row[0] != null && row[1] != null) {
                Difficulty diff = (Difficulty) row[0];
                difficultyStats.put(diff.name(), (Long) row[1]);
            }
        }
        stats.put("difficultyBreakdown", difficultyStats);

        // ── 3. Trợ lý AI (AI Cooking Assistant Activity) ──
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        long totalAi = aiSuggestionLogRepository.count();
        long todayAi = aiSuggestionLogRepository.countTodayCalls(startOfToday);
        long savedAi = aiSuggestionLogRepository.countSavedRecipes();
        Map<String, Long> aiTypeCounts = new LinkedHashMap<>();
        for (Object[] row : aiSuggestionLogRepository.countGroupByType()) {
            if (row[0] != null && row[1] != null) {
                aiTypeCounts.put(row[0].toString(), (Long) row[1]);
            }
        }
        Map<String, Object> aiStats = new LinkedHashMap<>();
        aiStats.put("totalCalls", totalAi);
        aiStats.put("todayCalls", todayAi);
        aiStats.put("savedRecipes", savedAi);
        aiStats.put("typeBreakdown", aiTypeCounts);
        stats.put("aiStats", aiStats);

        // ── 4. Tủ lạnh & Chống lãng phí (Inventory & Waste Prevention Index) ──
        long totalPantry = pantryRepository.count();
        LocalDate threeDaysAhead = LocalDate.now().plusDays(3);
        long expiringCount = pantryRepository.countExpiringSoon(threeDaysAhead);
        long safePantryCount = Math.max(0, totalPantry - expiringCount);
        int wastePreventionRate = totalPantry > 0 ? (int) Math.round(((double) safePantryCount / totalPantry) * 100) : 100;
        Map<String, Object> pantryStats = new LinkedHashMap<>();
        pantryStats.put("totalItems", totalPantry);
        pantryStats.put("expiringItems", expiringCount);
        pantryStats.put("safeItems", safePantryCount);
        pantryStats.put("preventionRate", wastePreventionRate);
        stats.put("pantryWasteStats", pantryStats);

        // ── 5. Hoạt động nấu nướng (Cooking Journal Activity) ──
        long totalJournals = cookingJournalRepository.count();
        Double avgRating = cookingJournalRepository.getPlatformAverageRating();
        Map<String, Object> journalStats = new LinkedHashMap<>();
        journalStats.put("totalJournals", totalJournals);
        journalStats.put("averageRating", avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 5.0);
        stats.put("cookingStats", journalStats);

        // ── 6. Hàng đợi kiểm duyệt nhanh (Quick Moderation Queue) ──
        List<Recipe> topPendingRecipes = recipeRepository.findTop3ByStatusOrderByCreatedAtDesc(RecipeStatus.PENDING_REVIEW);
        List<Map<String, Object>> pendingRecipeItems = topPendingRecipes.stream().map(r -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", r.getId());
            item.put("title", r.getTitle());
            item.put("imageUrl", r.getImageUrl());
            item.put("difficulty", r.getDifficulty() != null ? r.getDifficulty().name() : null);
            item.put("authorUsername", r.getAuthor() != null ? r.getAuthor().getUsername() : "—");
            item.put("createdAt", r.getCreatedAt());
            return item;
        }).toList();
        stats.put("recentPendingRecipes", pendingRecipeItems);

        // Top 3 nguyên liệu chờ duyệt
        Pageable top3 = PageRequest.of(0, 3, Sort.by("id").ascending());
        Page<Ingredient> topPendingIngs = ingredientRepository.findPendingReview(top3);
        List<Map<String, Object>> pendingIngItems = topPendingIngs.getContent().stream().map(ing -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", ing.getId());
            item.put("name", ing.getName());
            item.put("baseUnit", ing.getBaseUnit());
            item.put("aisleName", ing.getAisle() != null ? ing.getAisle().getName() : null);
            item.put("aisleId", ing.getAisle() != null ? ing.getAisle().getId() : null);
            return item;
        }).toList();
        stats.put("recentPendingIngredients", pendingIngItems);

        return ResponseEntity.ok(ApiResponse.success(stats, "Thống kê thành công"));
    }

    // ==================== RECIPE MODERATION ====================

    /**
     * Danh sách công thức lọc theo status (mặc định PENDING_REVIEW)
     */
    @GetMapping("/recipes")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAdminRecipes(
            @RequestParam(defaultValue = "PENDING_REVIEW") String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        RecipeStatus recipeStatus = RecipeStatus.valueOf(status);
        Pageable pageable = PageRequest.of(page, size);
        Page<Recipe> result;
        if (search != null && !search.trim().isEmpty()) {
            result = recipeRepository.findByStatusAndKeywordOrderByCreatedAtDesc(recipeStatus, search.trim(), pageable);
        } else {
            result = recipeRepository.findByStatusOrderByCreatedAtDesc(recipeStatus, pageable);
        }

        List<Map<String, Object>> items = result.getContent().stream().map(r -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", r.getId());
            item.put("title", r.getTitle());
            item.put("imageUrl", r.getImageUrl());
            item.put("status", r.getStatus().name());
            item.put("difficulty", r.getDifficulty() != null ? r.getDifficulty().name() : null);
            item.put("prepTime", r.getPrepTime());
            item.put("cookTime", r.getCookTime());
            item.put("ingredientCount", r.getIngredients() != null ? r.getIngredients().size() : 0);
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
        // KPI counts for recipe moderation
        response.put("pendingCount", recipeRepository.countByStatus(RecipeStatus.PENDING_REVIEW));
        response.put("publicCount", recipeRepository.countByStatus(RecipeStatus.PUBLIC));
        response.put("privateCount", recipeRepository.countByStatus(RecipeStatus.PRIVATE));
        response.put("totalCount", recipeRepository.countByStatusNot(RecipeStatus.DELETED));

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
            ing.put("id",              i.getIngredient().getId());
            ing.put("name",            i.getIngredient().getName());
            ing.put("amount",          i.getAmount());
            ing.put("unit",            i.getUnit());
            ing.put("baseUnit",        i.getIngredient().getBaseUnit());
            ing.put("caloriesPer100g", i.getIngredient().getCaloriesPer100g());
            ing.put("protein",         i.getIngredient().getProtein());
            ing.put("fat",             i.getIngredient().getFat());
            ing.put("carbs",           i.getIngredient().getCarbs());
            ing.put("aisleId",         i.getIngredient().getAisle() != null ? i.getIngredient().getAisle().getId() : null);
            ing.put("aisleName",       i.getIngredient().getAisle() != null ? i.getIngredient().getAisle().getName() : null);
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
        response.put("totalIngredients", ingredientRepository.count());
        response.put("pendingCount", ingredientRepository.countPendingReview());
        response.put("totalAisles", aisleRepository.count());

        return ResponseEntity.ok(ApiResponse.success(response, "Lấy danh sách nguyên liệu chờ duyệt thành công"));
    }

    /**
     * Tất cả nguyên liệu (phân trang, có tìm kiếm và lọc theo kệ hàng)
     */
    @GetMapping("/ingredients")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllIngredients(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer aisleId) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        Page<Ingredient> result;
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        boolean hasAisle = aisleId != null && aisleId > 0;

        if (hasKeyword && hasAisle) {
            result = ingredientRepository.findByNameContainingIgnoreCaseAndAisleId(keyword, aisleId, pageable);
        } else if (hasKeyword) {
            result = ingredientRepository.findByNameContainingIgnoreCase(keyword, pageable);
        } else if (hasAisle) {
            result = ingredientRepository.findByAisleId(aisleId, pageable);
        } else {
            result = ingredientRepository.findAll(pageable);
        }

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
        response.put("totalIngredients", ingredientRepository.count());
        response.put("pendingCount", ingredientRepository.countPendingReview());
        response.put("totalAisles", aisleRepository.count());

        return ResponseEntity.ok(ApiResponse.success(response, "Lấy danh sách nguyên liệu thành công"));
    }

    /**
     * Thêm mới nguyên liệu trực tiếp từ Admin (chuẩn hóa đầy đủ dinh dưỡng và kệ hàng)
     */
    @PostMapping("/ingredients")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createIngredient(
            @RequestBody Map<String, Object> body) {

        String name = body.get("name") != null ? body.get("name").toString().trim() : null;
        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Tên nguyên liệu không được để trống"));
        }

        String baseUnit = body.get("baseUnit") != null && !body.get("baseUnit").toString().isBlank()
                ? body.get("baseUnit").toString().trim() : "g";

        java.math.BigDecimal calories = body.get("caloriesPer100g") != null
                ? new java.math.BigDecimal(body.get("caloriesPer100g").toString()) : java.math.BigDecimal.ZERO;
        java.math.BigDecimal protein = body.get("protein") != null
                ? new java.math.BigDecimal(body.get("protein").toString()) : java.math.BigDecimal.ZERO;
        java.math.BigDecimal fat = body.get("fat") != null
                ? new java.math.BigDecimal(body.get("fat").toString()) : java.math.BigDecimal.ZERO;
        java.math.BigDecimal carbs = body.get("carbs") != null
                ? new java.math.BigDecimal(body.get("carbs").toString()) : java.math.BigDecimal.ZERO;

        Integer aisleId = null;
        if (body.get("aisleId") != null && !body.get("aisleId").toString().isBlank()) {
            aisleId = Integer.valueOf(body.get("aisleId").toString());
        }

        Ingredient ing = Ingredient.builder()
                .name(name)
                .baseUnit(baseUnit)
                .caloriesPer100g(calories)
                .protein(protein)
                .fat(fat)
                .carbs(carbs)
                .aisle(aisleId != null ? aisleRepository.findById(aisleId).orElse(null) : null)
                .build();

        ing = ingredientRepository.save(ing);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("id", ing.getId());
        res.put("name", ing.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(res, "Thêm nguyên liệu thành công"));
    }

    /**
     * Cập nhật dinh dưỡng và thông tin nguyên liệu
     */
    @PatchMapping("/ingredients/{id}")
    public ResponseEntity<ApiResponse<Void>> updateIngredient(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {

        Ingredient ing = ingredientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nguyên liệu ID: " + id));

        if (body.containsKey("name") && body.get("name") != null && !body.get("name").toString().isBlank())
            ing.setName(body.get("name").toString().trim());
        if (body.containsKey("baseUnit") && body.get("baseUnit") != null && !body.get("baseUnit").toString().isBlank())
            ing.setBaseUnit(body.get("baseUnit").toString().trim());
        if (body.containsKey("caloriesPer100g") && body.get("caloriesPer100g") != null)
            ing.setCaloriesPer100g(new java.math.BigDecimal(body.get("caloriesPer100g").toString()));
        if (body.containsKey("protein") && body.get("protein") != null)
            ing.setProtein(new java.math.BigDecimal(body.get("protein").toString()));
        if (body.containsKey("fat") && body.get("fat") != null)
            ing.setFat(new java.math.BigDecimal(body.get("fat").toString()));
        if (body.containsKey("carbs") && body.get("carbs") != null)
            ing.setCarbs(new java.math.BigDecimal(body.get("carbs").toString()));
        if (body.containsKey("aisleId")) {
            if (body.get("aisleId") != null && !body.get("aisleId").toString().isBlank()) {
                Integer aisleId = Integer.valueOf(body.get("aisleId").toString());
                ing.setAisle(aisleRepository.findById(aisleId).orElse(null));
            } else {
                ing.setAisle(null);
            }
        }

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

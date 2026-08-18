package com.smartrecipe.smartrecipe_backend.service.impl;

import com.smartrecipe.smartrecipe_backend.dto.request.CompleteGroceryRequest;
import com.smartrecipe.smartrecipe_backend.dto.request.GroceryItemRequest;
import com.smartrecipe.smartrecipe_backend.dto.request.GroceryListRequest;
import com.smartrecipe.smartrecipe_backend.dto.request.PantryRequest;
import com.smartrecipe.smartrecipe_backend.dto.response.AisleResponse;
import com.smartrecipe.smartrecipe_backend.dto.response.GroceryItemResponse;
import com.smartrecipe.smartrecipe_backend.dto.response.GroceryListResponse;
import com.smartrecipe.smartrecipe_backend.dto.response.IngredientResponse;
import com.smartrecipe.smartrecipe_backend.entity.*;
import com.smartrecipe.smartrecipe_backend.enums.GroceryListStatus;
import com.smartrecipe.smartrecipe_backend.exception.BadRequestException;
import com.smartrecipe.smartrecipe_backend.exception.ResourceNotFoundException;
import com.smartrecipe.smartrecipe_backend.repository.*;
import com.smartrecipe.smartrecipe_backend.service.GroceryService;
import com.smartrecipe.smartrecipe_backend.service.PantryService;
import com.smartrecipe.smartrecipe_backend.service.UnitNormalizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroceryServiceImpl implements GroceryService {

    private static final MathContext MC = MathContext.DECIMAL128;

    private final GroceryListRepository groceryListRepository;
    private final GroceryItemRepository groceryItemRepository;
    private final GroceryListRecipeRepository groceryListRecipeRepository;
    private final IngredientRepository ingredientRepository;
    private final RecipeRepository recipeRepository;
    private final RecipeIngredientRepository recipeIngredientRepository;
    private final PantryRepository pantryRepository;
    private final UnitNormalizationService unitNormalizationService;
    private final PantryService pantryService;
    private final UserRepository userRepository;

    // ---------- LIST CRUD ----------

    @Override
    @Transactional
    public GroceryListResponse createList(Long userId, GroceryListRequest request) {
        // One active list per user: return existing if present
        GroceryList existing = groceryListRepository
                .findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, GroceryListStatus.ACTIVE)
                .orElse(null);
        if (existing != null) {
            return toListResponse(existing);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
        GroceryList list = GroceryList.builder()
                .user(user)
                .status(GroceryListStatus.ACTIVE)
                .name(request.getName() != null && !request.getName().isBlank()
                        ? request.getName().trim()
                        : "Danh sách mua sắm")
                .build();
        list = groceryListRepository.save(list);
        return toListResponse(list);
    }

    @Override
    public List<GroceryListResponse> getMyLists(Long userId) {
        return groceryListRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toListResponse)
                .toList();
    }

    @Override
    public GroceryListResponse getActiveList(Long userId) {
        GroceryList existing = groceryListRepository
                .findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, GroceryListStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Không có danh sách đi chợ nào đang hoạt động"));
        return toListResponse(existing);
    }

    @Override
    public GroceryListResponse getList(Long userId, Long listId) {
        GroceryList list = groceryListRepository.findByIdAndUserId(listId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh sách"));
        return toListResponse(list);
    }

    @Override
    @Transactional
    public void deleteList(Long userId, Long listId) {
        GroceryList list = groceryListRepository.findByIdAndUserId(listId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh sách"));
        groceryItemRepository.deleteByGroceryListId(listId);
        groceryListRecipeRepository.deleteByGroceryListId(listId);
        groceryListRepository.delete(list);
    }

    @Override
    @Transactional
    public GroceryListResponse updateList(Long userId, Long listId, GroceryListRequest request) {
        GroceryList list = groceryListRepository.findByIdAndUserId(listId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh sách"));
        if (request.getName() != null && !request.getName().isBlank()) {
            list.setName(request.getName().trim());
        }
        list = groceryListRepository.save(list);
        return toListResponse(list);
    }

    @Override
    @Transactional
    public void clearItems(Long userId, Long listId) {
        GroceryList list = groceryListRepository.findByIdAndUserId(listId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh sách"));
        if (list.getStatus() != GroceryListStatus.ACTIVE) {
            throw new BadRequestException("Chỉ có thể xóa trong danh sách đang hoạt động");
        }
        groceryItemRepository.deleteByGroceryListId(listId);
    }

    // ---------- ITEM CRUD ----------

    @Override
    @Transactional
    public GroceryItemResponse addItem(Long userId, Long listId, GroceryItemRequest request) {
        GroceryList list = groceryListRepository.findByIdAndUserId(listId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh sách"));
        if (list.getStatus() != GroceryListStatus.ACTIVE) {
            throw new BadRequestException("Chỉ có thể thêm vào danh sách đang hoạt động");
        }

        Ingredient ingredient = ingredientRepository.findById(request.getIngredientId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nguyên liệu"));

        // Normalize requested quantity to base unit
        BigDecimal totalNeeded = unitNormalizationService.toBaseUnit(
                request.getQuantity(), request.getUnit(), ingredient);

        // Deduct what's in pantry (aggregate across lots)
        BigDecimal pantryDeducted = aggregatePantryQuantity(userId, ingredient.getId());
        BigDecimal finalToBuy = totalNeeded.subtract(pantryDeducted, MC);
        if (finalToBuy.compareTo(BigDecimal.ZERO) < 0) {
            finalToBuy = BigDecimal.ZERO;
        }

        // Merge with existing item for same ingredient
        GroceryItem existing = groceryItemRepository
                .findByGroceryListIdAndIngredientId(listId, ingredient.getId())
                .orElse(null);
        if (existing != null) {
            totalNeeded = totalNeeded.add(existing.getTotalNeeded(), MC);
            pantryDeducted = aggregatePantryQuantity(userId, ingredient.getId());
            finalToBuy = totalNeeded.subtract(pantryDeducted, MC);
            if (finalToBuy.compareTo(BigDecimal.ZERO) < 0) {
                finalToBuy = BigDecimal.ZERO;
            }
            existing.setTotalNeeded(totalNeeded);
            existing.setPantryDeducted(pantryDeducted);
            existing.setFinalToBuy(finalToBuy);
            return toItemResponse(groceryItemRepository.save(existing), ingredient);
        }

        GroceryItem item = GroceryItem.builder()
                .groceryList(list)
                .ingredient(ingredient)
                .totalNeeded(totalNeeded)
                .pantryDeducted(pantryDeducted)
                .finalToBuy(finalToBuy)
                .isBought(false)
                .build();
        return toItemResponse(groceryItemRepository.save(item), ingredient);
    }

    @Override
    @Transactional
    public GroceryItemResponse updateItem(Long userId, Long listId, Long itemId, GroceryItemRequest request) {
        GroceryList list = groceryListRepository.findByIdAndUserId(listId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh sách"));
        if (list.getStatus() != GroceryListStatus.ACTIVE) {
            throw new BadRequestException("Chỉ có thể sửa trong danh sách đang hoạt động");
        }
        GroceryItem item = groceryItemRepository.findByIdAndGroceryListId(itemId, listId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mục"));

        Ingredient ingredient = ingredientRepository.findById(request.getIngredientId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nguyên liệu"));

        BigDecimal totalNeeded = unitNormalizationService.toBaseUnit(
                request.getQuantity(), request.getUnit(), ingredient);

        BigDecimal pantryDeducted = aggregatePantryQuantity(userId, ingredient.getId());
        BigDecimal finalToBuy = totalNeeded.subtract(pantryDeducted, MC);
        if (finalToBuy.compareTo(BigDecimal.ZERO) < 0) {
            finalToBuy = BigDecimal.ZERO;
        }

        item.setIngredient(ingredient);
        item.setTotalNeeded(totalNeeded);
        item.setPantryDeducted(pantryDeducted);
        item.setFinalToBuy(finalToBuy);

        return toItemResponse(groceryItemRepository.save(item), ingredient);
    }

    @Override
    @Transactional
    public void removeItem(Long userId, Long listId, Long itemId) {
        GroceryList list = groceryListRepository.findByIdAndUserId(listId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh sách"));
        if (list.getStatus() != GroceryListStatus.ACTIVE) {
            throw new BadRequestException("Chỉ có thể xóa trong danh sách đang hoạt động");
        }
        GroceryItem item = groceryItemRepository.findByIdAndGroceryListId(itemId, listId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mục"));
        groceryItemRepository.delete(item);
    }

    @Override
    @Transactional
    public GroceryItemResponse togglePurchased(Long userId, Long listId, Long itemId) {
        GroceryList list = groceryListRepository.findByIdAndUserId(listId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh sách"));
        if (list.getStatus() != GroceryListStatus.ACTIVE) {
            throw new BadRequestException("Chỉ có thể đánh dấu trong danh sách đang hoạt động");
        }
        GroceryItem item = groceryItemRepository.findByIdAndGroceryListId(itemId, listId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mục"));
        item.setIsBought(!Boolean.TRUE.equals(item.getIsBought()));
        return toItemResponse(groceryItemRepository.save(item), item.getIngredient());
    }

    // ---------- COMPLETE ----------

    @Override
    @Transactional
    public GroceryListResponse completeList(Long userId, Long listId, CompleteGroceryRequest request) {
        GroceryList list = groceryListRepository.findByIdAndUserId(listId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh sách"));
        if (list.getStatus() != GroceryListStatus.ACTIVE) {
            throw new BadRequestException("Danh sách đã được hoàn tất trước đó");
        }

        list.setStatus(GroceryListStatus.COMPLETED);
        list.setCompletedAt(LocalDateTime.now());

        if (request.isAddToPantry()) {
            List<GroceryItem> items = groceryItemRepository
                    .findByGroceryListIdOrderByIngredient_Aisle_NameAsc(listId);
            for (GroceryItem item : items) {
                if (Boolean.TRUE.equals(item.getIsBought())
                        && item.getFinalToBuy().compareTo(BigDecimal.ZERO) > 0) {
                    PantryRequest pantryRequest = new PantryRequest();
                    pantryRequest.setIngredientId(item.getIngredient().getId());
                    pantryRequest.setQuantityAvailable(item.getFinalToBuy());
                    pantryRequest.setUnit(item.getIngredient().getBaseUnit());
                    
                    LocalDate expiry = null;
                    if (item.getIngredient().getAisle() != null) {
                        String aisleName = item.getIngredient().getAisle().getName();
                        if (aisleName.contains("Rau củ")) {
                            expiry = LocalDate.now().plusDays(4);
                        } else if (aisleName.contains("Thịt")) {
                            expiry = LocalDate.now().plusDays(3);
                        } else if (aisleName.contains("Sữa")) {
                            expiry = LocalDate.now().plusDays(7);
                        } else if (aisleName.contains("Gia vị")) {
                            expiry = LocalDate.now().plusMonths(3);
                        }
                    }
                    
                    pantryRequest.setExpiryDate(expiry);
                    pantryRequest.setLowStockThreshold(BigDecimal.ZERO);
                    pantryService.addOrUpdateItem(userId, pantryRequest);
                }
            }
        }

        list = groceryListRepository.save(list);
        return toListResponse(list);
    }

    // ---------- GENERATE ----------

    @Override
    @Transactional
    public GroceryListResponse generateFromRecipe(Long userId, Long recipeId, Integer servings) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công thức"));
        List<RecipeIngredient> recipeIngredients = recipeIngredientRepository.findByRecipeId(recipeId);

        // Get or create active list
        GroceryList list = groceryListRepository
                .findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, GroceryListStatus.ACTIVE)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
                    return groceryListRepository.save(GroceryList.builder()
                            .user(user)
                            .status(GroceryListStatus.ACTIVE)
                            .name("Từ " + recipe.getTitle())
                            .build());
                });

        // Record recipe-source (upsert)
        GroceryListRecipe existingLink = groceryListRecipeRepository.findByGroceryListId(list.getId()).stream()
                .filter(lr -> lr.getRecipe().getId().equals(recipeId))
                .findFirst().orElse(null);
        if (existingLink != null) {
            existingLink.setServings(servings);
            groceryListRecipeRepository.save(existingLink);
        } else {
            GroceryListRecipe newLink = GroceryListRecipe.builder()
                    .groceryList(list)
                    .recipe(recipe)
                    .servings(servings)
                    .build();
            groceryListRecipeRepository.save(newLink);
        }

        // Convert recipe ingredients (combine same ingredient)
        Map<Long, BigDecimal> aggregated = new LinkedHashMap<>();
        Map<Long, Ingredient> ingredientMap = new HashMap<>();
        for (RecipeIngredient ri : recipeIngredients) {
            Ingredient ing = ri.getIngredient();
            ingredientMap.put(ing.getId(), ing);
            BigDecimal inBase = unitNormalizationService.toBaseUnit(ri.getAmount(), ri.getUnit(), ing);
            aggregated.merge(ing.getId(), inBase, BigDecimal::add);
        }

        for (Map.Entry<Long, BigDecimal> entry : aggregated.entrySet()) {
            Long ingredientId = entry.getKey();
            Ingredient ing = ingredientMap.get(ingredientId);
            BigDecimal totalNeeded = entry.getValue();

            BigDecimal pantryDeducted = aggregatePantryQuantity(userId, ingredientId);
            BigDecimal finalToBuy = totalNeeded.subtract(pantryDeducted, MC);
            if (finalToBuy.compareTo(BigDecimal.ZERO) < 0) {
                finalToBuy = BigDecimal.ZERO;
            }

            GroceryItem existing = groceryItemRepository
                    .findByGroceryListIdAndIngredientId(list.getId(), ingredientId)
                    .orElse(null);
            if (existing != null) {
                totalNeeded = totalNeeded.add(existing.getTotalNeeded(), MC);
                pantryDeducted = aggregatePantryQuantity(userId, ingredientId);
                finalToBuy = totalNeeded.subtract(pantryDeducted, MC);
                if (finalToBuy.compareTo(BigDecimal.ZERO) < 0) {
                    finalToBuy = BigDecimal.ZERO;
                }
                existing.setTotalNeeded(totalNeeded);
                existing.setPantryDeducted(pantryDeducted);
                existing.setFinalToBuy(finalToBuy);
                groceryItemRepository.save(existing);
            } else {
                GroceryItem item = GroceryItem.builder()
                        .groceryList(list)
                        .ingredient(ing)
                        .totalNeeded(totalNeeded)
                        .pantryDeducted(pantryDeducted)
                        .finalToBuy(finalToBuy)
                        .isBought(false)
                        .build();
                groceryItemRepository.save(item);
            }
        }

        return toListResponse(list);
    }

    @Override
    @Transactional
    public GroceryListResponse generateFromPantry(Long userId) {
        List<UserPantry> pantryItems = pantryRepository.findByUserIdOrderByIngredient_Aisle_NameAscExpiryDateAsc(userId);

        // Aggregate by ingredient
        Map<Long, Ingredient> ingredientMap = new HashMap<>();
        // We'll generate items for ingredients at or below lowStockThreshold
        Map<Long, BigDecimal> lowStockNeeded = new LinkedHashMap<>();
        for (UserPantry up : pantryItems) {
            Ingredient ing = up.getIngredient();
            ingredientMap.put(ing.getId(), ing);
            // Simple heuristic: if any lot for this ingredient is below threshold, suggest buying
            if (up.getLowStockThreshold() != null
                    && up.getQuantityAvailable().compareTo(up.getLowStockThreshold()) <= 0) {
                lowStockNeeded.merge(ing.getId(), up.getLowStockThreshold().subtract(up.getQuantityAvailable(), MC),
                        BigDecimal::add);
            }
        }

        if (lowStockNeeded.isEmpty()) {
            throw new BadRequestException("Không có nguyên liệu nào dưới ngưỡng cần mua thêm");
        }

        GroceryList list = groceryListRepository
                .findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, GroceryListStatus.ACTIVE)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
                    return groceryListRepository.save(GroceryList.builder()
                            .user(user)
                            .status(GroceryListStatus.ACTIVE)
                            .name("Từ tủ nguyên liệu")
                            .build());
                });

        for (Map.Entry<Long, BigDecimal> entry : lowStockNeeded.entrySet()) {
            Long ingredientId = entry.getKey();
            Ingredient ing = ingredientMap.get(ingredientId);
            BigDecimal totalNeeded = entry.getValue();
            // already have some in pantry; no further deduction
            BigDecimal pantryDeducted = BigDecimal.ZERO;
            BigDecimal finalToBuy = totalNeeded;

            GroceryItem existing = groceryItemRepository
                    .findByGroceryListIdAndIngredientId(list.getId(), ingredientId)
                    .orElse(null);
            if (existing != null) {
                totalNeeded = totalNeeded.add(existing.getTotalNeeded(), MC);
                finalToBuy = totalNeeded;
                existing.setTotalNeeded(totalNeeded);
                existing.setPantryDeducted(BigDecimal.ZERO);
                existing.setFinalToBuy(finalToBuy);
                groceryItemRepository.save(existing);
            } else {
                GroceryItem item = GroceryItem.builder()
                        .groceryList(list)
                        .ingredient(ing)
                        .totalNeeded(totalNeeded)
                        .pantryDeducted(pantryDeducted)
                        .finalToBuy(finalToBuy)
                        .isBought(false)
                        .build();
                groceryItemRepository.save(item);
            }
        }

        return toListResponse(list);
    }

    // ---------- HELPERS ----------

    private BigDecimal aggregatePantryQuantity(Long userId, Long ingredientId) {
        List<UserPantry> lots = pantryRepository.findByUserIdAndIngredientIdOrderByExpiryDateAsc(userId, ingredientId);
        return lots.stream()
                .map(UserPantry::getQuantityAvailable)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private GroceryListResponse toListResponse(GroceryList list) {
        List<GroceryItem> items = groceryItemRepository
                .findByGroceryListIdOrderByIngredient_Aisle_NameAsc(list.getId());
        List<GroceryItemResponse> itemResponses = items.stream()
                .map(item -> toItemResponse(item, item.getIngredient()))
                .collect(Collectors.toList());
        long total = itemResponses.size();
        long purchased = itemResponses.stream().filter(gi -> Boolean.TRUE.equals(gi.getIsBought())).count();

        List<GroceryListResponse.RecipeSource> recipeSources = groceryListRecipeRepository
                .findByGroceryListId(list.getId()).stream()
                .map(glr -> GroceryListResponse.RecipeSource.builder()
                        .recipeId(glr.getRecipe().getId())
                        .recipeTitle(glr.getRecipe().getTitle())
                        .imageUrl(glr.getRecipe().getImageUrl())
                        .servings(glr.getServings())
                        .build())
                .collect(Collectors.toList());

        return GroceryListResponse.builder()
                .id(list.getId())
                .name(list.getName())
                .status(list.getStatus())
                .items(itemResponses)
                .recipeSources(recipeSources)
                .totalItems(total)
                .purchasedItems(purchased)
                .createdAt(list.getCreatedAt())
                .completedAt(list.getCompletedAt())
                .build();
    }

    private GroceryItemResponse toItemResponse(GroceryItem item, Ingredient ingredient) {
        IngredientResponse ingredientResponse = IngredientResponse.builder()
                .id(ingredient.getId())
                .name(ingredient.getName())
                .baseUnit(ingredient.getBaseUnit())
                .caloriesPer100g(ingredient.getCaloriesPer100g())
                .protein(ingredient.getProtein())
                .fat(ingredient.getFat())
                .carbs(ingredient.getCarbs())
                .aisle(ingredient.getAisle() != null
                        ? AisleResponse.builder().id(ingredient.getAisle().getId())
                            .name(ingredient.getAisle().getName()).build()
                        : null)
                .build();
        return GroceryItemResponse.builder()
                .id(item.getId())
                .ingredient(ingredientResponse)
                .totalNeeded(item.getTotalNeeded())
                .pantryDeducted(item.getPantryDeducted())
                .finalToBuy(item.getFinalToBuy())
                .unit(item.getIngredient().getBaseUnit())
                .isBought(item.getIsBought())
                .aisleName(item.getIngredient().getAisle() != null ? item.getIngredient().getAisle().getName() : null)
                .build();
    }
}
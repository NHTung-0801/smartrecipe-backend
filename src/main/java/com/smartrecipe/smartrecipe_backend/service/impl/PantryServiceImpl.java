package com.smartrecipe.smartrecipe_backend.service.impl;

import com.smartrecipe.smartrecipe_backend.dto.request.PantryRequest;
import com.smartrecipe.smartrecipe_backend.dto.response.AisleResponse;
import com.smartrecipe.smartrecipe_backend.dto.response.IngredientResponse;
import com.smartrecipe.smartrecipe_backend.dto.response.JournalResponse;
import com.smartrecipe.smartrecipe_backend.dto.response.PantryResponse;
import com.smartrecipe.smartrecipe_backend.dto.response.PantrySummaryResponse;
import com.smartrecipe.smartrecipe_backend.entity.Ingredient;
import com.smartrecipe.smartrecipe_backend.entity.Recipe;
import com.smartrecipe.smartrecipe_backend.entity.RecipeIngredient;
import com.smartrecipe.smartrecipe_backend.entity.User;
import com.smartrecipe.smartrecipe_backend.entity.UserPantry;
import com.smartrecipe.smartrecipe_backend.exception.BadRequestException;
import com.smartrecipe.smartrecipe_backend.exception.ResourceNotFoundException;
import com.smartrecipe.smartrecipe_backend.repository.IngredientRepository;
import com.smartrecipe.smartrecipe_backend.repository.PantryRepository;
import com.smartrecipe.smartrecipe_backend.repository.RecipeRepository;
import com.smartrecipe.smartrecipe_backend.repository.UserRepository;
import com.smartrecipe.smartrecipe_backend.service.PantryService;
import com.smartrecipe.smartrecipe_backend.service.UnitNormalizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PantryServiceImpl implements PantryService {

    private final PantryRepository pantryRepository;
    private final IngredientRepository ingredientRepository;
    private final UserRepository userRepository;
    private final RecipeRepository recipeRepository;
    private final UnitNormalizationService unitNormalizationService;

    @Override
    public PantryResponse addOrUpdateItem(Long userId, PantryRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId));
        Ingredient ingredient = ingredientRepository.findById(request.getIngredientId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nguyên liệu với ID: " + request.getIngredientId()));
        java.math.BigDecimal quantityBase = unitNormalizationService.toBaseUnit(
                request.getQuantityAvailable(), request.getUnit(), ingredient);
        java.math.BigDecimal thresholdBase = request.getLowStockThreshold() == null ? null
                : unitNormalizationService.toBaseUnit(request.getLowStockThreshold(), request.getUnit(), ingredient);

        if (request.getExpiryDate() == null) {
            request.setExpiryDate(calculateExpiryDate(ingredient));
        }

        List<UserPantry> existingLots = pantryRepository.findByUserIdAndIngredientIdOrderByExpiryDateAsc(userId, request.getIngredientId());

        if (!existingLots.isEmpty()) {
            UserPantry primaryLot = existingLots.get(0);
            
            // Dồn tất cả các dòng cũ vào dòng đầu tiên để đảm bảo chỉ có 1 dòng duy nhất
            if (existingLots.size() > 1) {
                for (int i = 1; i < existingLots.size(); i++) {
                    UserPantry dup = existingLots.get(i);
                    primaryLot.setQuantityAvailable(primaryLot.getQuantityAvailable().add(dup.getQuantityAvailable()));
                    pantryRepository.delete(dup);
                }
            }
            
            // Cộng thêm số lượng mới vào
            primaryLot.setQuantityAvailable(primaryLot.getQuantityAvailable().add(quantityBase));
            
            // Cập nhật lại hạn sử dụng theo lần thêm mới nhất
            if (request.getExpiryDate() != null) {
                primaryLot.setExpiryDate(request.getExpiryDate());
            }

            if (thresholdBase != null) {
                primaryLot.setLowStockThreshold(thresholdBase);
            }
            
            UserPantry saved = pantryRepository.save(primaryLot);
            return mapToResponse(saved);
        }

        UserPantry pantry = UserPantry.builder()
                .user(user)
                .ingredient(ingredient)
                .quantityAvailable(quantityBase)
                .lowStockThreshold(thresholdBase)
                .expiryDate(request.getExpiryDate())
                .build();

        UserPantry saved = pantryRepository.save(pantry);
        return mapToResponse(saved);
    }

    @Override
    public PantryResponse updateItem(Long userId, Long pantryId, PantryRequest request) {
        UserPantry pantry = pantryRepository.findByIdAndUserId(pantryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mục pantry với ID: " + pantryId));

        if (!pantry.getIngredient().getId().equals(request.getIngredientId())) {
            throw new BadRequestException("Không thể đổi nguyên liệu của một lot pantry");
        }
        java.math.BigDecimal quantityBase = unitNormalizationService.toBaseUnit(
                request.getQuantityAvailable(), request.getUnit(), pantry.getIngredient());
        java.math.BigDecimal thresholdBase = request.getLowStockThreshold() == null
                ? pantry.getLowStockThreshold()
                : unitNormalizationService.toBaseUnit(
                        request.getLowStockThreshold(), request.getUnit(), pantry.getIngredient());

        pantry.setQuantityAvailable(quantityBase);
        pantry.setExpiryDate(request.getExpiryDate());
        if (thresholdBase != null) {
            pantry.setLowStockThreshold(thresholdBase);
        }

        return mapToResponse(pantryRepository.save(pantry));
    }

    @Override
    public void removeItem(Long userId, Long pantryId) {
        UserPantry pantry = pantryRepository.findByIdAndUserId(pantryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mục pantry với ID: " + pantryId));
        pantryRepository.delete(pantry);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, List<PantryResponse>> getMyPantry(Long userId, String filter) {
        List<UserPantry> items = pantryRepository.findByUserIdOrderByIngredient_Aisle_NameAscExpiryDateAsc(userId);
        List<UserPantry> filteredItems = items;

        if (filter != null && filter.equalsIgnoreCase("LOW_STOCK")) {
            Set<Long> lowStockIngredientIds = findLowStockIngredientIds(items);
            filteredItems = items.stream()
                    .filter(item -> lowStockIngredientIds.contains(item.getIngredient().getId()))
                    .collect(Collectors.toList());
        }

        List<PantryResponse> responses = filteredItems.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        // Áp dụng filter
        if (filter != null && !filter.equalsIgnoreCase("ALL")) {
            responses = switch (filter.toUpperCase()) {
                case "EXPIRING_SOON" -> responses.stream()
                        .filter(r -> "EXPIRING_SOON".equals(r.getStatus()))
                        .collect(Collectors.toList());
                case "EXPIRED" -> responses.stream()
                        .filter(r -> "EXPIRED".equals(r.getStatus()))
                        .collect(Collectors.toList());
                case "LOW_STOCK" -> responses;
                default -> responses;
            };
        }

        // Nhóm theo aisle name
        return responses.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getAisleName() != null ? r.getAisleName() : "Chưa phân loại",
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PantryResponse> getExpiringSoon(Long userId, int days) {
        LocalDate today = LocalDate.now();
        LocalDate end = today.plusDays(days);
        return pantryRepository.findByUserIdAndExpiryDateBetween(userId, today, end)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteAllExpired(Long userId) {
        pantryRepository.deleteByUserIdAndExpiryDateBefore(userId, LocalDate.now());
    }

    @Override
    @Transactional(readOnly = true)
    public PantrySummaryResponse getPantrySummary(Long userId) {
        List<UserPantry> allItems = pantryRepository.findByUserIdOrderByIngredient_Aisle_NameAscExpiryDateAsc(userId);
        
        // Gộp các nguyên liệu cùng tên (case-insensitive) giống như frontend
        Map<String, UserPantry> mergedMap = new HashMap<>();
        for (UserPantry item : allItems) {
            if (item.getIngredient() == null || item.getIngredient().getName() == null) continue;
            String key = item.getIngredient().getName().toLowerCase();
            
            if (mergedMap.containsKey(key)) {
                UserPantry existing = mergedMap.get(key);
                existing.setQuantityAvailable(existing.getQuantityAvailable().add(item.getQuantityAvailable()));
                // Giữ lại hạn sử dụng gần nhất
                if (item.getExpiryDate() != null) {
                    if (existing.getExpiryDate() == null || item.getExpiryDate().isBefore(existing.getExpiryDate())) {
                        existing.setExpiryDate(item.getExpiryDate());
                    }
                }
                // Giữ lại ngưỡng cảnh báo lớn nhất
                if (item.getLowStockThreshold() != null) {
                    if (existing.getLowStockThreshold() == null || item.getLowStockThreshold().compareTo(existing.getLowStockThreshold()) > 0) {
                        existing.setLowStockThreshold(item.getLowStockThreshold());
                    }
                }
            } else {
                // Clone để tránh update vào DB khi transaction commit
                UserPantry copy = new UserPantry();
                copy.setQuantityAvailable(item.getQuantityAvailable());
                copy.setExpiryDate(item.getExpiryDate());
                copy.setLowStockThreshold(item.getLowStockThreshold());
                mergedMap.put(key, copy);
            }
        }

        LocalDate today = LocalDate.now();
        LocalDate soon = today.plusDays(7);

        long totalItems = mergedMap.size();
        long expiringSoonCount = 0;
        long expiredCount = 0;
        long lowStockCount = 0;

        for (UserPantry p : mergedMap.values()) {
            if (p.getExpiryDate() != null) {
                if (p.getExpiryDate().isBefore(today)) {
                    expiredCount++;
                } else if (!p.getExpiryDate().isAfter(soon)) {
                    expiringSoonCount++;
                }
            }
            if (p.getLowStockThreshold() != null && p.getQuantityAvailable().compareTo(p.getLowStockThreshold()) <= 0) {
                lowStockCount++;
            }
        }
        
        long freshCount = totalItems - expiringSoonCount - expiredCount;

        return PantrySummaryResponse.builder()
                .totalItems(totalItems)
                .expiringSoonCount(expiringSoonCount)
                .expiredCount(expiredCount)
                .lowStockCount(lowStockCount)
                .freshCount(freshCount)
                .build();
    }

    private PantryResponse mapToResponse(UserPantry pantry) {
        Ingredient ingredient = pantry.getIngredient();
        IngredientResponse ingredientResponse = null;
        String aisleName = null;

        if (ingredient != null) {
            AisleResponse aisleResponse = null;
            if (ingredient.getAisle() != null) {
                aisleName = ingredient.getAisle().getName();
                aisleResponse = AisleResponse.builder()
                        .id(ingredient.getAisle().getId())
                        .name(ingredient.getAisle().getName())
                        .build();
            }
            ingredientResponse = IngredientResponse.builder()
                    .id(ingredient.getId())
                    .name(ingredient.getName())
                    .baseUnit(ingredient.getBaseUnit())
                    .caloriesPer100g(ingredient.getCaloriesPer100g())
                    .protein(ingredient.getProtein())
                    .fat(ingredient.getFat())
                    .carbs(ingredient.getCarbs())
                    .aisle(aisleResponse)
                    .build();
        }

        return PantryResponse.builder()
                .id(pantry.getId())
                .ingredient(ingredientResponse)
                .quantityAvailable(pantry.getQuantityAvailable())
                .lowStockThreshold(pantry.getLowStockThreshold())
                .expiryDate(pantry.getExpiryDate())
                .aisleName(aisleName)
                .daysUntilExpiry(pantry.getExpiryDate() != null ?
                        java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), pantry.getExpiryDate()) : null)
                .status(computeStatus(pantry.getExpiryDate()))
                .build();
    }

    private String computeStatus(LocalDate expiryDate) {
        if (expiryDate == null) return "FRESH";
        long days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);
        if (days < 0) return "EXPIRED";
        if (days <= 7) return "EXPIRING_SOON";
        return "FRESH";
    }

    private LocalDate calculateExpiryDate(Ingredient ingredient) {
        if (ingredient.getAisle() != null) {
            String aisleName = ingredient.getAisle().getName();
            if (aisleName.contains("Rau củ")) {
                return LocalDate.now().plusDays(4);
            } else if (aisleName.contains("Thịt")) {
                return LocalDate.now().plusDays(3);
            } else if (aisleName.contains("Sữa")) {
                return LocalDate.now().plusDays(7);
            } else if (aisleName.contains("Gia vị")) {
                return LocalDate.now().plusMonths(3);
            }
        }
        return null;
    }

    private java.math.BigDecimal resolveThreshold(
            Long userId, Long ingredientId, java.math.BigDecimal requestedThreshold) {
        if (requestedThreshold != null) {
            updateThresholdForIngredient(userId, ingredientId, requestedThreshold);
            return requestedThreshold;
        }
        return pantryRepository.findByUserIdAndIngredientIdOrderByExpiryDateAsc(userId, ingredientId).stream()
                .map(UserPantry::getLowStockThreshold)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private void updateThresholdForIngredient(
            Long userId, Long ingredientId, java.math.BigDecimal threshold) {
        pantryRepository.findByUserIdAndIngredientIdOrderByExpiryDateAsc(userId, ingredientId)
                .forEach(lot -> lot.setLowStockThreshold(threshold));
    }

    private Set<Long> findLowStockIngredientIds(List<UserPantry> items) {
        Map<Long, java.math.BigDecimal> totals = items.stream()
                .collect(Collectors.groupingBy(item -> item.getIngredient().getId(),
                        Collectors.reducing(java.math.BigDecimal.ZERO,
                                UserPantry::getQuantityAvailable, java.math.BigDecimal::add)));
        return items.stream()
                .filter(item -> item.getLowStockThreshold() != null)
                .filter(item -> totals.get(item.getIngredient().getId())
                        .compareTo(item.getLowStockThreshold()) <= 0)
                .map(item -> item.getIngredient().getId())
                .collect(Collectors.toSet());
    }

    // ========== FEFO Deduction (Task 2.1) ==========

    @Override
    public List<JournalResponse.DeductionDetail> deductIngredientsForRecipe(
            Long userId, Long recipeId, int actualServings) {

        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy công thức với ID: " + recipeId));

        BigDecimal ratio = BigDecimal.valueOf(actualServings)
                .divide(BigDecimal.valueOf(recipe.getBaseServings()), MathContext.DECIMAL128);

        List<JournalResponse.DeductionDetail> deductions = new ArrayList<>();

        for (RecipeIngredient ri : recipe.getIngredients()) {
            Ingredient ingredient = ri.getIngredient();

            // Tính số lượng cần trừ, quy đổi về baseUnit
            BigDecimal requiredInBaseUnit;
            try {
                BigDecimal scaledAmount = ri.getAmount().multiply(ratio, MathContext.DECIMAL128);
                requiredInBaseUnit = unitNormalizationService.toBaseUnit(
                        scaledAmount, ri.getUnit(), ingredient);
            } catch (BadRequestException e) {
                // Edge case: đơn vị không quy đổi được → bỏ qua nguyên liệu này
                log.warn("Bỏ qua trừ kho cho '{}': {}", ingredient.getName(), e.getMessage());
                continue;
            }

            // Tìm các lô trong kho, sort theo expiryDate ASC (FEFO)
            List<UserPantry> lots = pantryRepository
                    .findByUserIdAndIngredientIdOrderByExpiryDateAsc(userId, ingredient.getId());

            if (lots.isEmpty()) {
                // Edge case: nguyên liệu không có trong kho → skip
                log.debug("Nguyên liệu '{}' không có trong kho, bỏ qua.", ingredient.getName());
                continue;
            }

            // Trừ dần theo FEFO
            BigDecimal remaining = requiredInBaseUnit;
            BigDecimal totalDeducted = BigDecimal.ZERO;

            Iterator<UserPantry> it = lots.iterator();
            while (remaining.signum() > 0 && it.hasNext()) {
                UserPantry lot = it.next();
                BigDecimal available = lot.getQuantityAvailable();

                if (available.compareTo(remaining) <= 0) {
                    // Lô này không đủ hoặc vừa đủ → dùng hết lô, xóa đi
                    totalDeducted = totalDeducted.add(available);
                    remaining = remaining.subtract(available);
                    pantryRepository.delete(lot);
                } else {
                    // Lô này dư → trừ bớt và giữ lại
                    totalDeducted = totalDeducted.add(remaining);
                    lot.setQuantityAvailable(available.subtract(remaining));
                    pantryRepository.save(lot);
                    remaining = BigDecimal.ZERO;
                }
            }

            // Ghi nhận kết quả trừ (nếu có trừ thực tế)
            if (totalDeducted.signum() > 0) {
                deductions.add(JournalResponse.DeductionDetail.builder()
                        .ingredientName(ingredient.getName())
                        .deductedAmount(totalDeducted.doubleValue())
                        .unit(ingredient.getBaseUnit())
                        .build());
            }
        }

        return deductions;
    }
}
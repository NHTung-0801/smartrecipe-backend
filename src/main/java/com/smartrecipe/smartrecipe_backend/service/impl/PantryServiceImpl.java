package com.smartrecipe.smartrecipe_backend.service.impl;

import com.smartrecipe.smartrecipe_backend.dto.request.PantryRequest;
import com.smartrecipe.smartrecipe_backend.dto.response.AisleResponse;
import com.smartrecipe.smartrecipe_backend.dto.response.IngredientResponse;
import com.smartrecipe.smartrecipe_backend.dto.response.PantryResponse;
import com.smartrecipe.smartrecipe_backend.dto.response.PantrySummaryResponse;
import com.smartrecipe.smartrecipe_backend.entity.Ingredient;
import com.smartrecipe.smartrecipe_backend.entity.User;
import com.smartrecipe.smartrecipe_backend.entity.UserPantry;
import com.smartrecipe.smartrecipe_backend.exception.ResourceNotFoundException;
import com.smartrecipe.smartrecipe_backend.repository.IngredientRepository;
import com.smartrecipe.smartrecipe_backend.repository.PantryRepository;
import com.smartrecipe.smartrecipe_backend.repository.UserRepository;
import com.smartrecipe.smartrecipe_backend.service.PantryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PantryServiceImpl implements PantryService {

    private final PantryRepository pantryRepository;
    private final IngredientRepository ingredientRepository;
    private final UserRepository userRepository;

    @Override
    public PantryResponse addOrUpdateItem(Long userId, PantryRequest request) {
        // Kiểm tra xem nguyên liệu đã có trong tủ chưa
        Optional<UserPantry> existing = pantryRepository.findByUserIdAndIngredientId(userId, request.getIngredientId());

        if (existing.isPresent()) {
            // Cộng dồn số lượng
            UserPantry pantry = existing.get();
            pantry.setQuantityAvailable(pantry.getQuantityAvailable().add(request.getQuantityAvailable()));
            // Cập nhật ngày hết hạn nếu có
            if (request.getExpiryDate() != null) {
                pantry.setExpiryDate(request.getExpiryDate());
            }
            // Cập nhật ngưỡng nếu có
            if (request.getLowStockThreshold() != null) {
                pantry.setLowStockThreshold(request.getLowStockThreshold());
            }
            UserPantry saved = pantryRepository.save(pantry);
            return mapToResponse(saved);
        }

        // Thêm mới
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId));
        Ingredient ingredient = ingredientRepository.findById(request.getIngredientId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nguyên liệu với ID: " + request.getIngredientId()));

        UserPantry pantry = UserPantry.builder()
                .user(user)
                .ingredient(ingredient)
                .quantityAvailable(request.getQuantityAvailable())
                .lowStockThreshold(request.getLowStockThreshold())
                .expiryDate(request.getExpiryDate())
                .build();

        UserPantry saved = pantryRepository.save(pantry);
        return mapToResponse(saved);
    }

    @Override
    public PantryResponse updateItem(Long pantryId, PantryRequest request) {
        UserPantry pantry = pantryRepository.findById(pantryId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mục pantry với ID: " + pantryId));

        pantry.setQuantityAvailable(request.getQuantityAvailable());
        pantry.setLowStockThreshold(request.getLowStockThreshold());
        pantry.setExpiryDate(request.getExpiryDate());

        UserPantry saved = pantryRepository.save(pantry);
        return mapToResponse(saved);
    }

    @Override
    public void removeItem(Long pantryId) {
        if (!pantryRepository.existsById(pantryId)) {
            throw new ResourceNotFoundException("Không tìm thấy mục pantry với ID: " + pantryId);
        }
        pantryRepository.deleteById(pantryId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, List<PantryResponse>> getMyPantry(Long userId, String filter) {
        List<UserPantry> items = pantryRepository.findByUserIdOrderByIngredient_Aisle_NameAscExpiryDateAsc(userId);

        List<PantryResponse> responses = items.stream()
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
                case "LOW_STOCK" -> items.stream()
                        .filter(p -> p.getLowStockThreshold() != null &&
                                p.getQuantityAvailable().compareTo(p.getLowStockThreshold()) <= 0)
                        .map(this::mapToResponse)
                        .collect(Collectors.toList());
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
}
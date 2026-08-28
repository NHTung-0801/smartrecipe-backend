package com.smartrecipe.smartrecipe_backend.service.impl;

import com.smartrecipe.smartrecipe_backend.dto.request.IngredientRequest;
import com.smartrecipe.smartrecipe_backend.dto.request.QuickIngredientRequest;
import com.smartrecipe.smartrecipe_backend.dto.response.AisleResponse;
import com.smartrecipe.smartrecipe_backend.dto.response.IngredientResponse;
import com.smartrecipe.smartrecipe_backend.entity.Aisle;
import com.smartrecipe.smartrecipe_backend.entity.Ingredient;
import com.smartrecipe.smartrecipe_backend.exception.ResourceNotFoundException;
import com.smartrecipe.smartrecipe_backend.repository.AisleRepository;
import com.smartrecipe.smartrecipe_backend.repository.IngredientRepository;
import com.smartrecipe.smartrecipe_backend.service.IngredientService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class IngredientServiceImpl implements IngredientService {

    private static final Logger log = LoggerFactory.getLogger(IngredientServiceImpl.class);

    private final IngredientRepository ingredientRepository;
    private final AisleRepository aisleRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<IngredientResponse> getAllIngredients(Pageable pageable) {
        return ingredientRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Cacheable(value = "ingredients_search", key = "#keyword")
    @Transactional(readOnly = true)
    public List<IngredientResponse> searchIngredients(String keyword) {
        return ingredientRepository.findByNameContainingIgnoreCase(keyword)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "ingredients_by_aisle", key = "#aisleId")
    @Transactional(readOnly = true)
    public List<IngredientResponse> getIngredientsByAisle(Integer aisleId) {
        return ingredientRepository.findByAisleId(aisleId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "ingredient", key = "#id")
    @Transactional(readOnly = true)
    public IngredientResponse getIngredientById(Long id) {
        Ingredient ingredient = ingredientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nguyên liệu với ID: " + id));
        return mapToResponse(ingredient);
    }

    @Override
    @CacheEvict(value = {"ingredients_search", "ingredients_by_aisle", "ingredient"}, allEntries = true)
    public IngredientResponse createIngredient(IngredientRequest request) {
        // Check if ingredient already exists to prevent duplicates
        java.util.Optional<Ingredient> existingOpt = ingredientRepository.findFirstByNameIgnoreCase(request.getName().trim());
        if (existingOpt.isPresent()) {
            Ingredient existing = existingOpt.get();
            // If existing ingredient doesn't have an aisle, but user provided one, update it
            if (existing.getAisle() == null && request.getAisleId() != null) {
                Aisle aisle = aisleRepository.findById(request.getAisleId()).orElse(null);
                if (aisle != null) {
                    existing.setAisle(aisle);
                    existing = ingredientRepository.save(existing);
                }
            }
            return mapToResponse(existing);
        }

        Ingredient ingredient = Ingredient.builder()
                .name(request.getName().trim())
                .baseUnit(request.getBaseUnit())
                .caloriesPer100g(request.getCaloriesPer100g())
                .protein(request.getProtein())
                .fat(request.getFat())
                .carbs(request.getCarbs())
                .build();

        if (request.getAisleId() != null) {
            Aisle aisle = aisleRepository.findById(request.getAisleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy quầy hàng với ID: " + request.getAisleId()));
            ingredient.setAisle(aisle);
        }

        Ingredient saved = ingredientRepository.save(ingredient);
        return mapToResponse(saved);
    }

    @Override
    @CacheEvict(value = {"ingredients_search", "ingredients_by_aisle", "ingredient"}, allEntries = true)
    public IngredientResponse createQuickIngredient(QuickIngredientRequest request) {
        String name = request.getName().trim();

        // Trả về bản có sẵn nếu trùng tên, giống createIngredient — tránh sinh
        // trùng lặp trong bảng 290 dòng vừa seed sạch.
        var existingOpt = ingredientRepository.findFirstByNameIgnoreCase(name);
        if (existingOpt.isPresent()) {
            Ingredient existing = existingOpt.get();
            if (existing.getAisle() == null && request.getAisleId() != null) {
                aisleRepository.findById(request.getAisleId()).ifPresent(existing::setAisle);
                existing = ingredientRepository.save(existing);
            }
            return mapToResponse(existing);
        }

        Ingredient ingredient = Ingredient.builder()
                .name(name)
                // 'g' cố định: user thường không được chọn đơn vị, vì đơn vị ngoài
                // 'g'/'ml' không có đường quy đổi trong unit_conversions.
                .baseUnit("g")
                // Dinh dưỡng = 0 là cờ "chờ admin kiểm duyệt". Admin lọc bằng
                // calories_per_100g = 0 (hiện chỉ có 'Muối' hợp lệ ở mức 0).
                .caloriesPer100g(BigDecimal.ZERO)
                .protein(BigDecimal.ZERO)
                .fat(BigDecimal.ZERO)
                .carbs(BigDecimal.ZERO)
                .build();

        if (request.getAisleId() != null) {
            Aisle aisle = aisleRepository.findById(request.getAisleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy quầy hàng với ID: " + request.getAisleId()));
            ingredient.setAisle(aisle);
        }

        Ingredient saved = ingredientRepository.save(ingredient);
        log.warn("User tự thêm nguyên liệu chưa có dinh dưỡng: id={} name='{}'", saved.getId(), saved.getName());
        return mapToResponse(saved);
    }

    @Override
    @CacheEvict(value = {"ingredients_search", "ingredients_by_aisle", "ingredient"}, allEntries = true)
    public IngredientResponse updateIngredient(Long id, IngredientRequest request) {
        Ingredient ingredient = ingredientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nguyên liệu với ID: " + id));

        ingredient.setName(request.getName());
        ingredient.setBaseUnit(request.getBaseUnit());
        ingredient.setCaloriesPer100g(request.getCaloriesPer100g());
        ingredient.setProtein(request.getProtein());
        ingredient.setFat(request.getFat());
        ingredient.setCarbs(request.getCarbs());

        if (request.getAisleId() != null) {
            Aisle aisle = aisleRepository.findById(request.getAisleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy quầy hàng với ID: " + request.getAisleId()));
            ingredient.setAisle(aisle);
        } else {
            ingredient.setAisle(null);
        }

        Ingredient saved = ingredientRepository.save(ingredient);
        return mapToResponse(saved);
    }

    @Override
    @CacheEvict(value = {"ingredients_search", "ingredients_by_aisle", "ingredient"}, allEntries = true)
    public IngredientResponse updateIngredientAisle(Long id, Integer aisleId) {
        Ingredient ingredient = ingredientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nguyên liệu với ID: " + id));

        Aisle aisle = null;
        if (aisleId != null) {
            aisle = aisleRepository.findById(aisleId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy quầy hàng với ID: " + aisleId));
        }
        
        ingredient.setAisle(aisle);
        Ingredient saved = ingredientRepository.save(ingredient);
        return mapToResponse(saved);
    }

    @Override
    @CacheEvict(value = {"ingredients_search", "ingredients_by_aisle", "ingredient"}, allEntries = true)
    public void deleteIngredient(Long id) {
        if (!ingredientRepository.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy nguyên liệu với ID: " + id);
        }
        ingredientRepository.deleteById(id);
    }

    private IngredientResponse mapToResponse(Ingredient ingredient) {
        AisleResponse aisleResponse = null;
        if (ingredient.getAisle() != null) {
            aisleResponse = AisleResponse.builder()
                    .id(ingredient.getAisle().getId())
                    .name(ingredient.getAisle().getName())
                    .build();
        }

        return IngredientResponse.builder()
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
}
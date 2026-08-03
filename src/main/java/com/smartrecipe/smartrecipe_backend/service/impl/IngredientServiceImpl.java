package com.smartrecipe.smartrecipe_backend.service.impl;

import com.smartrecipe.smartrecipe_backend.dto.request.IngredientRequest;
import com.smartrecipe.smartrecipe_backend.dto.response.AisleResponse;
import com.smartrecipe.smartrecipe_backend.dto.response.IngredientResponse;
import com.smartrecipe.smartrecipe_backend.entity.Aisle;
import com.smartrecipe.smartrecipe_backend.entity.Ingredient;
import com.smartrecipe.smartrecipe_backend.exception.ResourceNotFoundException;
import com.smartrecipe.smartrecipe_backend.repository.AisleRepository;
import com.smartrecipe.smartrecipe_backend.repository.IngredientRepository;
import com.smartrecipe.smartrecipe_backend.service.IngredientService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class IngredientServiceImpl implements IngredientService {

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
        Ingredient ingredient = Ingredient.builder()
                .name(request.getName())
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
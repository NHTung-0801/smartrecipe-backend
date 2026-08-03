package com.smartrecipe.smartrecipe_backend.service.impl;

import com.smartrecipe.smartrecipe_backend.dto.request.UnitConversionRequest;
import com.smartrecipe.smartrecipe_backend.dto.response.UnitConversionResponse;
import com.smartrecipe.smartrecipe_backend.entity.Ingredient;
import com.smartrecipe.smartrecipe_backend.entity.UnitConversion;
import com.smartrecipe.smartrecipe_backend.exception.ResourceNotFoundException;
import com.smartrecipe.smartrecipe_backend.repository.IngredientRepository;
import com.smartrecipe.smartrecipe_backend.repository.UnitConversionRepository;
import com.smartrecipe.smartrecipe_backend.service.UnitConversionService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UnitConversionServiceImpl implements UnitConversionService {

    private final UnitConversionRepository unitConversionRepository;
    private final IngredientRepository ingredientRepository;

    @Override
    @Cacheable("unit_conversions")
    @Transactional(readOnly = true)
    public List<UnitConversionResponse> getAllConversions() {
        return unitConversionRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "unit_conversions_by_ingredient", key = "#ingredientId")
    @Transactional(readOnly = true)
    public List<UnitConversionResponse> getConversionsByIngredient(Long ingredientId) {
        return unitConversionRepository.findByIngredientId(ingredientId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable("unit_conversions_generic")
    @Transactional(readOnly = true)
    public List<UnitConversionResponse> getGenericConversions() {
        return unitConversionRepository.findByIngredientIsNull()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @CacheEvict(value = {"unit_conversions", "unit_conversions_by_ingredient", "unit_conversions_generic"}, allEntries = true)
    public UnitConversionResponse createConversion(UnitConversionRequest request) {
        UnitConversion conversion = UnitConversion.builder()
                .fromUnit(request.getFromUnit())
                .toUnit(request.getToUnit())
                .multiplier(request.getMultiplier())
                .build();

        if (request.getIngredientId() != null) {
            Ingredient ingredient = ingredientRepository.findById(request.getIngredientId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nguyên liệu với ID: " + request.getIngredientId()));
            conversion.setIngredient(ingredient);
        }

        UnitConversion saved = unitConversionRepository.save(conversion);
        return mapToResponse(saved);
    }

    @Override
    @CacheEvict(value = {"unit_conversions", "unit_conversions_by_ingredient", "unit_conversions_generic"}, allEntries = true)
    public UnitConversionResponse updateConversion(Integer id, UnitConversionRequest request) {
        UnitConversion conversion = unitConversionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy quy đổi đơn vị với ID: " + id));

        conversion.setFromUnit(request.getFromUnit());
        conversion.setToUnit(request.getToUnit());
        conversion.setMultiplier(request.getMultiplier());

        if (request.getIngredientId() != null) {
            Ingredient ingredient = ingredientRepository.findById(request.getIngredientId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nguyên liệu với ID: " + request.getIngredientId()));
            conversion.setIngredient(ingredient);
        } else {
            conversion.setIngredient(null);
        }

        UnitConversion saved = unitConversionRepository.save(conversion);
        return mapToResponse(saved);
    }

    @Override
    @CacheEvict(value = {"unit_conversions", "unit_conversions_by_ingredient", "unit_conversions_generic"}, allEntries = true)
    public void deleteConversion(Integer id) {
        if (!unitConversionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy quy đổi đơn vị với ID: " + id);
        }
        unitConversionRepository.deleteById(id);
    }

    private UnitConversionResponse mapToResponse(UnitConversion conversion) {
        return UnitConversionResponse.builder()
                .id(conversion.getId())
                .fromUnit(conversion.getFromUnit())
                .toUnit(conversion.getToUnit())
                .multiplier(conversion.getMultiplier())
                .ingredientId(conversion.getIngredient() != null ? conversion.getIngredient().getId() : null)
                .ingredientName(conversion.getIngredient() != null ? conversion.getIngredient().getName() : null)
                .build();
    }
}
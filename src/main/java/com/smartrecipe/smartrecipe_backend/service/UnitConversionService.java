package com.smartrecipe.smartrecipe_backend.service;

import com.smartrecipe.smartrecipe_backend.dto.request.UnitConversionRequest;
import com.smartrecipe.smartrecipe_backend.dto.response.UnitConversionResponse;

import java.util.List;

public interface UnitConversionService {
    List<UnitConversionResponse> getAllConversions();
    List<UnitConversionResponse> getConversionsByIngredient(Long ingredientId);
    List<UnitConversionResponse> getGenericConversions();
    UnitConversionResponse createConversion(UnitConversionRequest request);
    UnitConversionResponse updateConversion(Integer id, UnitConversionRequest request);
    void deleteConversion(Integer id);
}
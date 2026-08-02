package com.smartrecipe.smartrecipe_backend.service;

import com.smartrecipe.smartrecipe_backend.dto.request.IngredientRequest;
import com.smartrecipe.smartrecipe_backend.dto.response.IngredientResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IngredientService {
    Page<IngredientResponse> getAllIngredients(Pageable pageable);
    List<IngredientResponse> searchIngredients(String keyword);
    List<IngredientResponse> getIngredientsByAisle(Integer aisleId);
    IngredientResponse getIngredientById(Long id);
    IngredientResponse createIngredient(IngredientRequest request);
    IngredientResponse updateIngredient(Long id, IngredientRequest request);
    void deleteIngredient(Long id);
}
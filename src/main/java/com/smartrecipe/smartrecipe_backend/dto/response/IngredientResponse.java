package com.smartrecipe.smartrecipe_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngredientResponse {
    private Long id;
    private String name;
    private String baseUnit;
    private BigDecimal caloriesPer100g;
    private BigDecimal protein;
    private BigDecimal fat;
    private BigDecimal carbs;
    private AisleResponse aisle;
}
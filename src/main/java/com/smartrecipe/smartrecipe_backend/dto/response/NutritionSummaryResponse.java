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
public class NutritionSummaryResponse {
    private BigDecimal totalCalories;
    private BigDecimal totalProtein;
    private BigDecimal totalFat;
    private BigDecimal totalCarbs;
    private BigDecimal caloriesPerServing;
    private BigDecimal proteinPerServing;
    private BigDecimal fatPerServing;
    private BigDecimal carbsPerServing;
}
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
public class RecipeIngredientResponse {
    private Long id;
    private Long ingredientId;
    private String ingredientName;
    private String ingredientImageUrl;
    private BigDecimal amount;
    private String unit;
    private Long aisleId;
    private String aisleName;
}
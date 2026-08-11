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
public class GroceryItemResponse {
    private Long id;
    private IngredientResponse ingredient;
    private BigDecimal totalNeeded;
    private BigDecimal pantryDeducted;
    private BigDecimal finalToBuy;
    private String unit;
    private Boolean isBought;
    private String aisleName;
}

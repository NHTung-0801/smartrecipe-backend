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
public class UnitConversionResponse {
    private Integer id;
    private String fromUnit;
    private String toUnit;
    private BigDecimal multiplier;
    private Long ingredientId;
    private String ingredientName;
}
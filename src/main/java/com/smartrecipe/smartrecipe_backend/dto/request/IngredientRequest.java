package com.smartrecipe.smartrecipe_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IngredientRequest {

    @NotBlank(message = "Tên nguyên liệu không được để trống")
    private String name;

    @NotBlank(message = "Đơn vị cơ bản không được để trống")
    private String baseUnit;

    @PositiveOrZero(message = "Calo không được âm")
    private BigDecimal caloriesPer100g;

    @PositiveOrZero(message = "Protein không được âm")
    private BigDecimal protein;

    @PositiveOrZero(message = "Chất béo không được âm")
    private BigDecimal fat;

    @PositiveOrZero(message = "Carbs không được âm")
    private BigDecimal carbs;

    private Integer aisleId;
}
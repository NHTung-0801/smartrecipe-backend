package com.smartrecipe.smartrecipe_backend.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeIngredientRequest {

    @NotNull(message = "ID nguyên liệu không được để trống")
    private Long ingredientId;

    @NotNull(message = "Số lượng không được để trống")
    @DecimalMin(value = "0.01", message = "Số lượng phải lớn hơn 0")
    private BigDecimal amount;

    @NotBlank(message = "Đơn vị không được để trống")
    private String unit;
}
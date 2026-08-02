package com.smartrecipe.smartrecipe_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnitConversionRequest {

    @NotBlank(message = "Đơn vị gốc không được để trống")
    private String fromUnit;

    @NotBlank(message = "Đơn vị đích không được để trống")
    private String toUnit;

    @Positive(message = "Hệ số nhân phải lớn hơn 0")
    private BigDecimal multiplier;

    private Long ingredientId;
}
package com.smartrecipe.smartrecipe_backend.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeStepRequest {

    @NotNull(message = "Số thứ tự bước không được để trống")
    @Min(value = 1, message = "Số thứ tự bước phải lớn hơn 0")
    private Integer stepNumber;

    @NotBlank(message = "Hướng dẫn không được để trống")
    private String instruction;
}
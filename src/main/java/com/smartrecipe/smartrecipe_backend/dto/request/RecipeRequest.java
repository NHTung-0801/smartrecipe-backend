package com.smartrecipe.smartrecipe_backend.dto.request;

import com.smartrecipe.smartrecipe_backend.enums.Difficulty;
import com.smartrecipe.smartrecipe_backend.enums.RecipeStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeRequest {

    @NotBlank(message = "Tiêu đề công thức không được để trống")
    @Size(max = 255, message = "Tiêu đề không được vượt quá 255 ký tự")
    private String title;

    @Size(max = 2000, message = "Mô tả không được vượt quá 2000 ký tự")
    private String description;

    @NotNull(message = "Số suất ăn cơ bản không được để trống")
    @Min(value = 1, message = "Số suất ăn phải lớn hơn 0")
    private Integer baseServings;

    @NotNull(message = "Trạng thái không được để trống")
    private RecipeStatus status;

    private String imageUrl;

    @Min(value = 1, message = "Thời gian chuẩn bị phải lớn hơn 0")
    private Integer prepTime;

    @Min(value = 0, message = "Thời gian nấu không được âm")
    private Integer cookTime;

    private Difficulty difficulty;

    @Valid
    @Size(min = 1, message = "Cần ít nhất 1 bước nấu")
    private List<RecipeStepRequest> steps;

    @Valid
    @Size(min = 1, message = "Cần ít nhất 1 nguyên liệu")
    private List<RecipeIngredientRequest> ingredients;

    private List<Integer> tagIds;
}
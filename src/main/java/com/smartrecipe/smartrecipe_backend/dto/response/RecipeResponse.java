package com.smartrecipe.smartrecipe_backend.dto.response;

import com.smartrecipe.smartrecipe_backend.enums.Difficulty;
import com.smartrecipe.smartrecipe_backend.enums.RecipeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeResponse {
    private Long id;
    private String title;
    private String description;
    private Integer baseServings;
    private RecipeStatus status;
    private String imageUrl;
    private Integer prepTime;
    private Integer cookTime;
    private Difficulty difficulty;
    private Integer likeCount;
    private Long clonedFromId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private AuthorSummaryResponse author;
    private List<RecipeStepResponse> steps;
    private List<RecipeIngredientResponse> ingredients;
    private List<TagResponse> tags;
    private NutritionSummaryResponse nutrition;
}
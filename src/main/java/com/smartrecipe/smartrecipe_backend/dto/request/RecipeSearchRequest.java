package com.smartrecipe.smartrecipe_backend.dto.request;

import com.smartrecipe.smartrecipe_backend.enums.Difficulty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeSearchRequest {
    private String keyword;
    private List<Integer> tagIds;
    private Difficulty difficulty;
    private String sortBy; // newest, popular, prepTime, cookTime
}
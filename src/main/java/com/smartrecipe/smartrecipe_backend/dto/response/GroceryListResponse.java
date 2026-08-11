package com.smartrecipe.smartrecipe_backend.dto.response;

import com.smartrecipe.smartrecipe_backend.enums.GroceryListStatus;
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
public class GroceryListResponse {
    private Long id;
    private String name;
    private GroceryListStatus status;
    private List<GroceryItemResponse> items;
    private List<RecipeSource> recipeSources;
    private long totalItems;
    private long purchasedItems;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecipeSource {
        private Long recipeId;
        private String recipeTitle;
        private String imageUrl;
        private Integer servings;
    }
}

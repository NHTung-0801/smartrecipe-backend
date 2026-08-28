package com.smartrecipe.smartrecipe_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response DTO trả về cho frontend sau khi gọi AI gợi ý công thức.
 * Chứa toàn bộ thông tin công thức + logId để phục vụ lưu công thức.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiSuggestResponse {

    /** ID của AiSuggestionLog — dùng khi user muốn lưu thành công thức thật */
    private Long logId;

    /** Tên món ăn */
    private String title;

    /** Mô tả món ăn */
    private String description;

    /** Số suất ăn cơ bản */
    private Integer baseServings;

    /** Thời gian chuẩn bị (phút) */
    private Integer prepTime;

    /** Thời gian nấu (phút) */
    private Integer cookTime;

    /** Độ khó: EASY, MEDIUM, HARD */
    private String difficulty;

    /** Danh sách nguyên liệu */
    private List<IngredientItem> ingredients;

    /** Danh sách các bước nấu */
    private List<StepItem> steps;

    /** User có thể lưu thành công thức không (luôn true nếu gọi thành công) */
    private boolean canSave;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IngredientItem {
        private String ingredientName;
        private BigDecimal amount;
        private String unit;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StepItem {
        private Integer stepNumber;
        private String instruction;
    }
}

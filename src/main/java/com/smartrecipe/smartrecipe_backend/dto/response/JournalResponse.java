package com.smartrecipe.smartrecipe_backend.dto.response;

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
public class JournalResponse {

    private Long id;

    /** Tóm tắt công thức đã nấu (id, title, imageUrl). */
    private RecipeSummaryInfo recipe;

    private LocalDateTime cookedAt;
    private Integer actualServings;
    private Integer rating;
    private String iterationNotes;
    private String imageUrl;
    
    /** Đánh giá trung bình của user cho công thức này. */
    private Double averageRating;

    /** Danh sách nguyên liệu đã trừ khỏi kho (chỉ có khi vừa tạo mới). */
    private List<DeductionDetail> deductionSummary;

    // ========== Inner DTOs ==========

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecipeSummaryInfo {
        private Long id;
        private String title;
        private String imageUrl;
        private Integer baseServings;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeductionDetail {
        private String ingredientName;
        private Double deductedAmount;
        private String unit;
    }
}

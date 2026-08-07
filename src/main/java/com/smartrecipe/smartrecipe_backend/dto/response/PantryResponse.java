package com.smartrecipe.smartrecipe_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PantryResponse {
    private Long id;
    private IngredientResponse ingredient;
    private BigDecimal quantityAvailable;
    private BigDecimal lowStockThreshold;
    private LocalDate expiryDate;
    private Long daysUntilExpiry;
    private String status;
    private String aisleName;

    /**
     * Tính số ngày còn lại đến hạn sử dụng.
     * null nếu expiryDate == null (không có hạn).
     * Âm nếu đã hết hạn.
     */
    public Long getDaysUntilExpiry() {
        if (expiryDate == null) return null;
        return ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);
    }

    /**
     * Tính trạng thái tự động dựa trên expiryDate:
     * - FRESH: expiryDate == null hoặc còn > 7 ngày
     * - EXPIRING_SOON: expiryDate trong vòng 1-7 ngày tới
     * - EXPIRED: expiryDate < hôm nay
     */
    public String getStatus() {
        if (expiryDate == null) return "FRESH";
        Long days = getDaysUntilExpiry();
        if (days == null) return "FRESH";
        if (days < 0) return "EXPIRED";
        if (days <= 7) return "EXPIRING_SOON";
        return "FRESH";
    }
}
package com.smartrecipe.smartrecipe_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PantrySummaryResponse {
    private long totalItems;
    private long expiringSoonCount;
    private long expiredCount;
    private long lowStockCount;
    private long freshCount;
}
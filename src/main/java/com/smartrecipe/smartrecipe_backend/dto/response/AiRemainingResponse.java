package com.smartrecipe.smartrecipe_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Số lượt gợi ý AI còn lại trong ngày của user.
 * Frontend dùng để hiển thị "còn 7/10 lượt" và disable nút trước khi gọi API,
 * thay vì phải chờ lỗi 429.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiRemainingResponse {

    /** Số lượt đã dùng kể từ 00:00 hôm nay */
    private long used;

    /** Giới hạn mỗi ngày (gemini.daily-limit) */
    private int dailyLimit;

    /** Số lượt còn lại, không bao giờ âm */
    private long remaining;
}

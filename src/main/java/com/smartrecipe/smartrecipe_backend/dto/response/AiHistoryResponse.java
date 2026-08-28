package com.smartrecipe.smartrecipe_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Một dòng trong lịch sử gợi ý AI của user.
 *
 * <p>Chỉ trả về phần tóm tắt (tên món, nguyên liệu đầu vào, đã lưu chưa) thay vì
 * toàn bộ JSON thô trong `ai_suggestion_logs.output_response` — danh sách lịch
 * sử không cần từng bước nấu. Muốn xem chi tiết thì mở công thức đã lưu.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiHistoryResponse {

    /** ID của AiSuggestionLog — dùng cho POST /ai/save/{logId} nếu chưa lưu */
    private Long logId;

    /** ZERO_WASTE (từ tủ lạnh) hoặc FEASIBLE_FINDER (nhập tay) */
    private String type;

    /** Tên món AI đã gợi ý, đọc từ output_response */
    private String title;

    /** Nguyên liệu đầu vào lúc đó, dạng text như đã gửi cho AI */
    private String inputIngredients;

    /** ID công thức đã lưu, null nghĩa là user chưa lưu gợi ý này */
    private Long savedRecipeId;

    /** true khi gợi ý này còn có thể lưu thành công thức */
    private boolean canSave;

    private LocalDateTime createdAt;
}

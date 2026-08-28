package com.smartrecipe.smartrecipe_backend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO cho chế độ "Custom Input" — người dùng tự nhập nguyên liệu.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiSuggestRequest {

    /**
     * Danh sách nguyên liệu do người dùng nhập tay.
     * Ví dụ: ["Thịt bò", "Hành tây", "Cà chua"]
     */
    private List<String> ingredients;
}

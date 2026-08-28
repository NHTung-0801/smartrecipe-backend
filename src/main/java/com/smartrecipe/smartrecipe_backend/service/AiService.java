package com.smartrecipe.smartrecipe_backend.service;

import com.smartrecipe.smartrecipe_backend.dto.response.AiHistoryResponse;
import com.smartrecipe.smartrecipe_backend.dto.response.AiRemainingResponse;
import com.smartrecipe.smartrecipe_backend.dto.response.AiSuggestResponse;
import com.smartrecipe.smartrecipe_backend.dto.response.RecipeResponse;

import java.util.List;

public interface AiService {

    /**
     * Gợi ý công thức từ tủ lạnh (Zero-Waste).
     * Lấy nguyên liệu trong pantry, ưu tiên sắp hết hạn, gọi AI và trả về kết quả.
     *
     * @param userId ID người dùng hiện tại
     * @return Công thức được AI gợi ý
     */
    AiSuggestResponse suggestFromPantry(Long userId);

    /**
     * Gợi ý công thức từ nguyên liệu nhập tay (Custom Input).
     *
     * @param userId      ID người dùng hiện tại
     * @param ingredients Danh sách nguyên liệu dạng text
     * @return Công thức được AI gợi ý
     */
    AiSuggestResponse suggestFromInput(Long userId, List<String> ingredients);

    /**
     * Lưu kết quả gợi ý AI thành công thức thật trong hệ thống.
     * Match ingredientName → ingredientId từ bảng ingredients trong DB.
     *
     * @param userId ID người dùng hiện tại
     * @param logId  ID của AiSuggestionLog cần lưu
     * @return RecipeResponse của công thức vừa tạo
     */
    RecipeResponse saveAiRecipe(Long userId, Long logId);

    /**
     * Lấy lịch sử gợi ý AI của user, mới nhất trước.
     *
     * @param userId ID người dùng hiện tại
     * @return danh sách log dạng tóm tắt, rỗng nếu user chưa dùng AI lần nào
     */
    List<AiHistoryResponse> getHistory(Long userId);

    /**
     * Số lượt gợi ý AI còn lại trong ngày hôm nay của user.
     * Frontend gọi trước để hiển thị và disable nút, không cần chờ lỗi 429.
     *
     * @param userId ID người dùng hiện tại
     * @return số đã dùng, giới hạn ngày và số còn lại
     */
    AiRemainingResponse getRemaining(Long userId);
}

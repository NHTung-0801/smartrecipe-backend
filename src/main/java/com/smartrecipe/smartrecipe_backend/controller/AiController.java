package com.smartrecipe.smartrecipe_backend.controller;

import com.smartrecipe.smartrecipe_backend.dto.request.AiSuggestRequest;
import com.smartrecipe.smartrecipe_backend.dto.response.AiHistoryResponse;
import com.smartrecipe.smartrecipe_backend.dto.response.AiRemainingResponse;
import com.smartrecipe.smartrecipe_backend.dto.response.AiSuggestResponse;
import com.smartrecipe.smartrecipe_backend.dto.response.ApiResponse;
import com.smartrecipe.smartrecipe_backend.dto.response.RecipeResponse;
import com.smartrecipe.smartrecipe_backend.exception.ResourceNotFoundException;
import com.smartrecipe.smartrecipe_backend.repository.UserRepository;
import com.smartrecipe.smartrecipe_backend.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;
    private final UserRepository userRepository;

    private Long getUserId(Principal principal) {
        return userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"))
                .getId();
    }

    /**
     * Gợi ý công thức từ tủ lạnh (Zero-Waste).
     * Tự động lấy nguyên liệu trong pantry, ưu tiên sắp hết hạn.
     */
    @GetMapping("/suggest/pantry")
    public ResponseEntity<ApiResponse<AiSuggestResponse>> suggestFromPantry(Principal principal) {
        Long userId = getUserId(principal);
        AiSuggestResponse result = aiService.suggestFromPantry(userId);
        return ResponseEntity.ok(ApiResponse.success(result, "Gợi ý công thức thành công!"));
    }

    /**
     * Gợi ý công thức từ nguyên liệu nhập tay (Custom Input).
     */
    @PostMapping("/suggest/custom")
    public ResponseEntity<ApiResponse<AiSuggestResponse>> suggestFromInput(
            Principal principal,
            @RequestBody AiSuggestRequest request) {
        Long userId = getUserId(principal);
        AiSuggestResponse result = aiService.suggestFromInput(userId, request.getIngredients());
        return ResponseEntity.ok(ApiResponse.success(result, "Gợi ý công thức thành công!"));
    }

    /**
     * Lưu kết quả gợi ý AI thành công thức thật trong hệ thống.
     * Backend sẽ match ingredientName → ingredientId từ DB.
     */
    @PostMapping("/save/{logId}")
    public ResponseEntity<ApiResponse<RecipeResponse>> saveAiRecipe(
            Principal principal,
            @PathVariable Long logId) {
        Long userId = getUserId(principal);
        RecipeResponse saved = aiService.saveAiRecipe(userId, logId);
        return ResponseEntity.ok(ApiResponse.success(saved, "Đã lưu công thức từ gợi ý AI!"));
    }

    /**
     * Lịch sử gợi ý AI của user hiện tại, mới nhất trước.
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<AiHistoryResponse>>> getHistory(Principal principal) {
        Long userId = getUserId(principal);
        List<AiHistoryResponse> history = aiService.getHistory(userId);
        return ResponseEntity.ok(ApiResponse.success(history, "Lấy lịch sử gợi ý AI thành công!"));
    }

    /**
     * Số lượt gợi ý AI còn lại hôm nay.
     * Frontend gọi khi mở trang để hiển thị "còn N/M lượt" và disable nút khi hết,
     * thay vì để user bấm rồi nhận lỗi 429.
     */
    @GetMapping("/remaining")
    public ResponseEntity<ApiResponse<AiRemainingResponse>> getRemaining(Principal principal) {
        Long userId = getUserId(principal);
        AiRemainingResponse remaining = aiService.getRemaining(userId);
        return ResponseEntity.ok(ApiResponse.success(remaining, "Số lượt gợi ý AI còn lại hôm nay!"));
    }
}

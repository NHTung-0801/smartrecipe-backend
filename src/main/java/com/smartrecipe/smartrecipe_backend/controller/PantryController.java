package com.smartrecipe.smartrecipe_backend.controller;

import com.smartrecipe.smartrecipe_backend.dto.request.PantryRequest;
import com.smartrecipe.smartrecipe_backend.dto.response.ApiResponse;
import com.smartrecipe.smartrecipe_backend.dto.response.PantryResponse;
import com.smartrecipe.smartrecipe_backend.dto.response.PantrySummaryResponse;
import com.smartrecipe.smartrecipe_backend.exception.ResourceNotFoundException;
import com.smartrecipe.smartrecipe_backend.repository.UserRepository;
import com.smartrecipe.smartrecipe_backend.service.PantryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/pantry")
@RequiredArgsConstructor
public class PantryController {

    private final PantryService pantryService;
    private final UserRepository userRepository;

    private Long getUserId(Principal principal) {
        return userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"))
                .getId();
    }

    /** GET /api/v1/pantry - Lấy danh sách pantry, nhóm theo Aisle, hỗ trợ filter */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, List<PantryResponse>>>> getMyPantry(
            Principal principal,
            @RequestParam(defaultValue = "ALL") String filter) {
        Long userId = getUserId(principal);
        Map<String, List<PantryResponse>> result = pantryService.getMyPantry(userId, filter);
        return ResponseEntity.ok(ApiResponse.success(result, "Lấy danh sách pantry thành công!"));
    }

    /** GET /api/v1/pantry/summary - Tổng quan trạng thái pantry */
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<PantrySummaryResponse>> getPantrySummary(Principal principal) {
        Long userId = getUserId(principal);
        PantrySummaryResponse summary = pantryService.getPantrySummary(userId);
        return ResponseEntity.ok(ApiResponse.success(summary, "Lấy tổng quan pantry thành công!"));
    }

    /** GET /api/v1/pantry/expiring-soon - Nguyên liệu sắp hết hạn trong N ngày */
    @GetMapping("/expiring-soon")
    public ResponseEntity<ApiResponse<List<PantryResponse>>> getExpiringSoon(
            Principal principal,
            @RequestParam(defaultValue = "7") int days) {
        Long userId = getUserId(principal);
        List<PantryResponse> result = pantryService.getExpiringSoon(userId, days);
        return ResponseEntity.ok(ApiResponse.success(result, "Danh sách nguyên liệu sắp hết hạn!"));
    }

    /** POST /api/v1/pantry - Thêm/cập nhật (cộng dồn) nguyên liệu vào tủ */
    @PostMapping
    public ResponseEntity<ApiResponse<PantryResponse>> addOrUpdateItem(
            @Valid @RequestBody PantryRequest request,
            Principal principal) {
        Long userId = getUserId(principal);
        PantryResponse response = pantryService.addOrUpdateItem(userId, request);
        return new ResponseEntity<>(ApiResponse.success(response, "Đã thêm/cập nhật nguyên liệu vào tủ!"), HttpStatus.CREATED);
    }

    /** PUT /api/v1/pantry/{id} - Cập nhật số lượng/ngưỡng/hạn sử dụng */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PantryResponse>> updateItem(
            @PathVariable Long id,
            @Valid @RequestBody PantryRequest request) {
        PantryResponse response = pantryService.updateItem(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật mục pantry thành công!"));
    }

    /** DELETE /api/v1/pantry/{id} - Xóa 1 nguyên liệu khỏi tủ */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> removeItem(@PathVariable Long id) {
        pantryService.removeItem(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã xóa nguyên liệu khỏi tủ!"));
    }

    /** DELETE /api/v1/pantry/expired - Xóa toàn bộ nguyên liệu đã hết hạn */
    @DeleteMapping("/expired")
    public ResponseEntity<ApiResponse<Void>> deleteAllExpired(Principal principal) {
        Long userId = getUserId(principal);
        pantryService.deleteAllExpired(userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã xóa tất cả nguyên liệu hết hạn!"));
    }
}
package com.smartrecipe.smartrecipe_backend.service;

import com.smartrecipe.smartrecipe_backend.dto.request.PantryRequest;
import com.smartrecipe.smartrecipe_backend.dto.response.PantryResponse;
import com.smartrecipe.smartrecipe_backend.dto.response.PantrySummaryResponse;

import java.util.List;
import java.util.Map;

public interface PantryService {

    /** Thêm mới hoặc cộng dồn nếu đã có. */
    PantryResponse addOrUpdateItem(Long userId, PantryRequest request);

    /** Cập nhật số lượng/ngưỡng/ngày hết hạn. */
    PantryResponse updateItem(Long pantryId, PantryRequest request);

    /** Xóa 1 nguyên liệu khỏi tủ. */
    void removeItem(Long pantryId);

    /** Lấy danh sách pantry của user, nhóm theo Aisle. filter: ALL / EXPIRING_SOON / EXPIRED / LOW_STOCK. */
    Map<String, List<PantryResponse>> getMyPantry(Long userId, String filter);

    /** Lấy danh sách nguyên liệu sắp hết hạn trong N ngày tới. */
    List<PantryResponse> getExpiringSoon(Long userId, int days);

    /** Xóa toàn bộ nguyên liệu đã hết hạn. */
    void deleteAllExpired(Long userId);

    /** Lấy tổng quan trạng thái pantry. */
    PantrySummaryResponse getPantrySummary(Long userId);
}
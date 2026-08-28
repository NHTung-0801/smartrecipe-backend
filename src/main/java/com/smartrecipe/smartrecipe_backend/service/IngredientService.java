package com.smartrecipe.smartrecipe_backend.service;

import com.smartrecipe.smartrecipe_backend.dto.request.IngredientRequest;
import com.smartrecipe.smartrecipe_backend.dto.request.QuickIngredientRequest;
import com.smartrecipe.smartrecipe_backend.dto.response.IngredientResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IngredientService {
    Page<IngredientResponse> getAllIngredients(Pageable pageable);
    List<IngredientResponse> searchIngredients(String keyword);
    List<IngredientResponse> getIngredientsByAisle(Integer aisleId);
    IngredientResponse getIngredientById(Long id);
    IngredientResponse createIngredient(IngredientRequest request);

    /**
     * Thêm nhanh nguyên liệu cho user thường: chỉ nhận tên + kệ hàng.
     * Server tự đặt {@code baseUnit = 'g'} và dinh dưỡng = 0 (cờ chờ kiểm duyệt).
     * Nếu tên đã tồn tại thì trả về nguyên liệu cũ thay vì tạo bản trùng.
     */
    IngredientResponse createQuickIngredient(QuickIngredientRequest request);

    IngredientResponse updateIngredient(Long id, IngredientRequest request);
    IngredientResponse updateIngredientAisle(Long id, Integer aisleId);
    void deleteIngredient(Long id);
}
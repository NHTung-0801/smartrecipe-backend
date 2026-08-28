package com.smartrecipe.smartrecipe_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request cho user thường tự thêm nhanh một nguyên liệu chưa có trong hệ thống
 * (khi thêm vào tủ nguyên liệu hoặc danh sách đi chợ).
 *
 * <p>Cố tình KHÔNG có các trường dinh dưỡng và {@code baseUnit}: user thường không
 * biết calo/protein của nguyên liệu, và {@code baseUnit} phải là 'g'/'ml' để
 * {@code UnitNormalizationService} quy đổi được. Server tự đặt dinh dưỡng = 0 làm
 * cờ "chờ admin kiểm duyệt" và {@code baseUnit = 'g'}.
 *
 * <p>Muốn nhập đầy đủ dinh dưỡng thì dùng {@code POST /api/v1/ingredients}
 * (chỉ ADMIN) hoặc {@code PUT /api/v1/ingredients/{id}}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuickIngredientRequest {

    @NotBlank(message = "Tên nguyên liệu không được để trống")
    @Size(max = 100, message = "Tên nguyên liệu không được vượt quá 100 ký tự")
    private String name;

    /** Kệ hàng để nhóm trong danh sách đi chợ; có thể để trống */
    private Integer aisleId;
}

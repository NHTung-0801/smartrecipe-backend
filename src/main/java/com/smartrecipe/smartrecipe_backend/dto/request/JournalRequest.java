package com.smartrecipe.smartrecipe_backend.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalRequest {

    @NotNull(message = "ID công thức không được để trống")
    private Long recipeId;

    @NotNull(message = "Số khẩu phần thực tế không được để trống")
    @Min(value = 1, message = "Số khẩu phần phải lớn hơn 0")
    private Integer actualServings;

    @Min(value = 1, message = "Đánh giá tối thiểu là 1 sao")
    @Max(value = 5, message = "Đánh giá tối đa là 5 sao")
    private Integer rating;

    /** Ghi chú cá nhân (VD: "Lần sau cho ít muối hơn"). */
    private String iterationNotes;

    /** URL ảnh thành quả (upload qua Cloudinary trước khi gửi). */
    private String imageUrl;
}

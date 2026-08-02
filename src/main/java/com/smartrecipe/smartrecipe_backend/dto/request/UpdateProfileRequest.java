package com.smartrecipe.smartrecipe_backend.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    @Size(max = 100, message = "Tên hiển thị không được vượt quá 100 ký tự")
    private String displayName;

    @Size(max = 500, message = "Tiểu sử không được vượt quá 500 ký tự")
    private String bio;
}

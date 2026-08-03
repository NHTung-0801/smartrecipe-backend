package com.smartrecipe.smartrecipe_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TagRequest {

    @NotBlank(message = "Tên tag không được để trống")
    @Size(max = 50, message = "Tên tag không được vượt quá 50 ký tự")
    private String name;
}

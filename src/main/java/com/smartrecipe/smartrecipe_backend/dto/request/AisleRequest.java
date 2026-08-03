package com.smartrecipe.smartrecipe_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AisleRequest {

    @NotBlank(message = "Tên quầy hàng không được để trống")
    @Size(max = 100, message = "Tên quầy hàng không được vượt quá 100 ký tự")
    private String name;
}

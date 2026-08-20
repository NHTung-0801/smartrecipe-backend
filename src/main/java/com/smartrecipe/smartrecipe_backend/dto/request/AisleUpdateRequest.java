package com.smartrecipe.smartrecipe_backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AisleUpdateRequest {

    @NotNull(message = "ID của quầy hàng không được để trống")
    private Integer aisleId;
}

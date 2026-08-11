package com.smartrecipe.smartrecipe_backend.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroceryListRequest {

    @Size(max = 100, message = "Tên danh sách không được vượt quá 100 ký tự")
    private String name;
}

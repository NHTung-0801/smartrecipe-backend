package com.smartrecipe.smartrecipe_backend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompleteGroceryRequest {
    private boolean addToPantry;
}

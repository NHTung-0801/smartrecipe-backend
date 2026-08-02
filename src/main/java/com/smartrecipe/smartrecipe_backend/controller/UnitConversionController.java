package com.smartrecipe.smartrecipe_backend.controller;

import com.smartrecipe.smartrecipe_backend.dto.request.UnitConversionRequest;
import com.smartrecipe.smartrecipe_backend.dto.response.ApiResponse;
import com.smartrecipe.smartrecipe_backend.dto.response.UnitConversionResponse;
import com.smartrecipe.smartrecipe_backend.service.UnitConversionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/unit-conversions")
@RequiredArgsConstructor
public class UnitConversionController {

    private final UnitConversionService unitConversionService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<UnitConversionResponse>>> getAllConversions() {
        List<UnitConversionResponse> conversions = unitConversionService.getAllConversions();
        return ResponseEntity.ok(ApiResponse.success(conversions, "Lấy danh sách quy đổi đơn vị thành công!"));
    }

    @GetMapping("/generic")
    public ResponseEntity<ApiResponse<List<UnitConversionResponse>>> getGenericConversions() {
        List<UnitConversionResponse> conversions = unitConversionService.getGenericConversions();
        return ResponseEntity.ok(ApiResponse.success(conversions, "Danh sách quy đổi đơn vị chung!"));
    }

    @GetMapping("/ingredient/{ingredientId}")
    public ResponseEntity<ApiResponse<List<UnitConversionResponse>>> getConversionsByIngredient(@PathVariable Long ingredientId) {
        List<UnitConversionResponse> conversions = unitConversionService.getConversionsByIngredient(ingredientId);
        return ResponseEntity.ok(ApiResponse.success(conversions, "Danh sách quy đổi đơn vị cho nguyên liệu!"));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UnitConversionResponse>> createConversion(@Valid @RequestBody UnitConversionRequest request) {
        UnitConversionResponse conversion = unitConversionService.createConversion(request);
        return new ResponseEntity<>(ApiResponse.success(conversion, "Tạo quy đổi đơn vị thành công!"), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UnitConversionResponse>> updateConversion(
            @PathVariable Integer id, @Valid @RequestBody UnitConversionRequest request) {
        UnitConversionResponse conversion = unitConversionService.updateConversion(id, request);
        return ResponseEntity.ok(ApiResponse.success(conversion, "Cập nhật quy đổi đơn vị thành công!"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteConversion(@PathVariable Integer id) {
        unitConversionService.deleteConversion(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Xóa quy đổi đơn vị thành công!"));
    }
}
package com.smartrecipe.smartrecipe_backend.controller;

import com.smartrecipe.smartrecipe_backend.dto.response.AisleResponse;
import com.smartrecipe.smartrecipe_backend.dto.response.ApiResponse;
import com.smartrecipe.smartrecipe_backend.service.AisleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/aisles")
@RequiredArgsConstructor
public class AisleController {

    private final AisleService aisleService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AisleResponse>>> getAllAisles() {
        List<AisleResponse> aisles = aisleService.getAllAisles();
        return ResponseEntity.ok(ApiResponse.success(aisles, "Lấy danh sách quầy hàng thành công!"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AisleResponse>> getAisleById(@PathVariable Integer id) {
        AisleResponse aisle = aisleService.getAisleById(id);
        return ResponseEntity.ok(ApiResponse.success(aisle, "Chi tiết quầy hàng!"));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AisleResponse>> createAisle(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        AisleResponse aisle = aisleService.createAisle(name);
        return new ResponseEntity<>(ApiResponse.success(aisle, "Tạo quầy hàng thành công!"), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AisleResponse>> updateAisle(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        String name = body.get("name");
        AisleResponse aisle = aisleService.updateAisle(id, name);
        return ResponseEntity.ok(ApiResponse.success(aisle, "Cập nhật quầy hàng thành công!"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteAisle(@PathVariable Integer id) {
        aisleService.deleteAisle(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Xóa quầy hàng thành công!"));
    }
}
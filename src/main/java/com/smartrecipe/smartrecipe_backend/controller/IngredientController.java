package com.smartrecipe.smartrecipe_backend.controller;

import com.smartrecipe.smartrecipe_backend.dto.request.IngredientRequest;
import com.smartrecipe.smartrecipe_backend.dto.response.ApiResponse;
import com.smartrecipe.smartrecipe_backend.dto.response.IngredientResponse;
import com.smartrecipe.smartrecipe_backend.service.IngredientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ingredients")
@RequiredArgsConstructor
public class IngredientController {

    private final IngredientService ingredientService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<IngredientResponse>>> getAllIngredients(
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<IngredientResponse> ingredients = ingredientService.getAllIngredients(pageable);
        return ResponseEntity.ok(ApiResponse.success(ingredients, "Lấy danh sách nguyên liệu thành công!"));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<IngredientResponse>>> searchIngredients(@RequestParam String q) {
        List<IngredientResponse> ingredients = ingredientService.searchIngredients(q);
        return ResponseEntity.ok(ApiResponse.success(ingredients, "Kết quả tìm kiếm cho: " + q));
    }

    @GetMapping("/aisle/{aisleId}")
    public ResponseEntity<ApiResponse<List<IngredientResponse>>> getIngredientsByAisle(@PathVariable Integer aisleId) {
        List<IngredientResponse> ingredients = ingredientService.getIngredientsByAisle(aisleId);
        return ResponseEntity.ok(ApiResponse.success(ingredients, "Danh sách nguyên liệu theo quầy hàng!"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<IngredientResponse>> getIngredientById(@PathVariable Long id) {
        IngredientResponse ingredient = ingredientService.getIngredientById(id);
        return ResponseEntity.ok(ApiResponse.success(ingredient, "Chi tiết nguyên liệu!"));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<IngredientResponse>> createIngredient(@Valid @RequestBody IngredientRequest request) {
        IngredientResponse ingredient = ingredientService.createIngredient(request);
        return new ResponseEntity<>(ApiResponse.success(ingredient, "Tạo nguyên liệu thành công!"), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<IngredientResponse>> updateIngredient(
            @PathVariable Long id, @Valid @RequestBody IngredientRequest request) {
        IngredientResponse ingredient = ingredientService.updateIngredient(id, request);
        return ResponseEntity.ok(ApiResponse.success(ingredient, "Cập nhật nguyên liệu thành công!"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteIngredient(@PathVariable Long id) {
        ingredientService.deleteIngredient(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Xóa nguyên liệu thành công!"));
    }
}
package com.smartrecipe.smartrecipe_backend.controller;

import com.smartrecipe.smartrecipe_backend.dto.request.RecipeRequest;
import com.smartrecipe.smartrecipe_backend.dto.request.RecipeSearchRequest;
import com.smartrecipe.smartrecipe_backend.dto.response.ImageUploadResponse;
import com.smartrecipe.smartrecipe_backend.dto.response.RecipeResponse;
import com.smartrecipe.smartrecipe_backend.dto.response.RecipeSummaryResponse;
import com.smartrecipe.smartrecipe_backend.exception.ResourceNotFoundException;
import com.smartrecipe.smartrecipe_backend.repository.UserRepository;
import com.smartrecipe.smartrecipe_backend.service.RecipeExportService;
import com.smartrecipe.smartrecipe_backend.service.RecipeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/recipes")
@RequiredArgsConstructor
public class RecipeController {

    private final RecipeService recipeService;
    private final RecipeExportService recipeExportService;
    private final UserRepository userRepository;

    /**
     * Helper lấy userId từ Principal.
     */
    private Long getUserId(Principal principal) {
        return userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"))
                .getId();
    }

    // ==================== CRUD ====================

    @PostMapping
    public ResponseEntity<RecipeResponse> createRecipe(
            @Valid @RequestBody RecipeRequest request,
            Principal principal) {
        Long userId = getUserId(principal);
        RecipeResponse response = recipeService.createRecipe(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecipeResponse> getRecipeById(@PathVariable Long id) {
        RecipeResponse response = recipeService.getRecipeById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RecipeResponse> updateRecipe(
            @PathVariable Long id,
            @Valid @RequestBody RecipeRequest request,
            Principal principal) {
        Long userId = getUserId(principal);
        RecipeResponse response = recipeService.updateRecipe(id, request, userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecipe(
            @PathVariable Long id,
            Principal principal) {
        Long userId = getUserId(principal);
        recipeService.deleteRecipe(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<RecipeResponse> changeRecipeStatus(
            @PathVariable Long id,
            @RequestParam com.smartrecipe.smartrecipe_backend.enums.RecipeStatus status,
            Principal principal) {
        Long userId = getUserId(principal);
        RecipeResponse response = recipeService.changeStatus(id, status, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/export/word")
    public ResponseEntity<byte[]> exportRecipeToWord(@PathVariable Long id) {
        byte[] docBytes = recipeExportService.exportRecipeToWord(id);
        
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        headers.setContentDispositionFormData("attachment", "recipe_" + id + ".docx");
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
        
        return new ResponseEntity<>(docBytes, headers, HttpStatus.OK);
    }

    // ==================== LISTING & SEARCH ====================

    @GetMapping("/my")
    public ResponseEntity<Page<RecipeSummaryResponse>> getMyRecipes(
            Principal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = getUserId(principal);
        Page<RecipeSummaryResponse> response = recipeService.getMyRecipes(userId, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/public")
    public ResponseEntity<Page<RecipeSummaryResponse>> getPublicRecipes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<RecipeSummaryResponse> response = recipeService.getPublicRecipes(page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<Page<RecipeSummaryResponse>> searchRecipes(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        RecipeSearchRequest request = RecipeSearchRequest.builder()
                .keyword(keyword)
                .build();
        Page<RecipeSummaryResponse> response = recipeService.searchRecipes(request, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<RecipeSummaryResponse>> getUserPublicRecipes(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<RecipeSummaryResponse> response = recipeService.getUserPublicRecipes(userId, page, size);
        return ResponseEntity.ok(response);
    }

    // ==================== CLONE ====================

    @PostMapping("/{id}/clone")
    public ResponseEntity<RecipeResponse> cloneRecipe(
            @PathVariable Long id,
            Principal principal) {
        Long userId = getUserId(principal);
        RecipeResponse response = recipeService.cloneRecipe(id, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ==================== LIKE / UNLIKE ====================

    @PostMapping("/{id}/like")
    public ResponseEntity<Void> likeRecipe(
            @PathVariable Long id,
            Principal principal) {
        Long userId = getUserId(principal);
        recipeService.likeRecipe(id, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/like")
    public ResponseEntity<Void> unlikeRecipe(
            @PathVariable Long id,
            Principal principal) {
        Long userId = getUserId(principal);
        recipeService.unlikeRecipe(id, userId);
        return ResponseEntity.noContent().build();
    }

    // ==================== UPLOAD IMAGE ====================

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageUploadResponse> uploadRecipeImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            Principal principal) {
        Long userId = getUserId(principal);
        ImageUploadResponse response = recipeService.uploadRecipeImage(id, file, userId);
        return ResponseEntity.ok(response);
    }
}

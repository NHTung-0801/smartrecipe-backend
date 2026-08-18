package com.smartrecipe.smartrecipe_backend.controller;

import com.smartrecipe.smartrecipe_backend.dto.request.CompleteGroceryRequest;
import com.smartrecipe.smartrecipe_backend.dto.request.GroceryItemRequest;
import com.smartrecipe.smartrecipe_backend.dto.request.GroceryListRequest;
import com.smartrecipe.smartrecipe_backend.dto.response.GroceryItemResponse;
import com.smartrecipe.smartrecipe_backend.dto.response.GroceryListResponse;
import com.smartrecipe.smartrecipe_backend.repository.UserRepository;
import com.smartrecipe.smartrecipe_backend.exception.ResourceNotFoundException;
import com.smartrecipe.smartrecipe_backend.service.GroceryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/grocery")
@RequiredArgsConstructor
public class GroceryController {

    private final GroceryService groceryService;
    private final UserRepository userRepository;

    private Long getUserId(Principal principal) {
        return userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"))
                .getId();
    }

    // ---------- LIST ENDPOINTS ----------

    @GetMapping("/lists")
    public ResponseEntity<Map<String, Object>> getAllLists(Principal principal) {
        List<GroceryListResponse> lists = groceryService.getMyLists(getUserId(principal));
        return ResponseEntity.ok(Map.of(
                "data", lists,
                "message", "Lấy danh sách đi chợ thành công"
        ));
    }

    @GetMapping("/lists/active")
    public ResponseEntity<Map<String, Object>> getActiveList(Principal principal) {
        GroceryListResponse list = groceryService.getActiveList(getUserId(principal));
        return ResponseEntity.ok(Map.of(
                "data", list,
                "message", "Lấy danh sách đang hoạt động thành công"
        ));
    }

    @PostMapping("/lists")
    public ResponseEntity<Map<String, Object>> createList(
            Principal principal,
            @Valid @RequestBody GroceryListRequest request) {
        GroceryListResponse list = groceryService.createList(getUserId(principal), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "data", list,
                "message", "Tạo danh sách đi chợ thành công"
        ));
    }

    @GetMapping("/lists/{id}")
    public ResponseEntity<Map<String, Object>> getListById(
            Principal principal,
            @PathVariable Long id) {
        GroceryListResponse list = groceryService.getList(getUserId(principal), id);
        return ResponseEntity.ok(Map.of(
                "data", list,
                "message", "Lấy chi tiết danh sách thành công"
        ));
    }

    @DeleteMapping("/lists/{id}")
    public ResponseEntity<Map<String, Object>> deleteList(
            Principal principal,
            @PathVariable Long id) {
        groceryService.deleteList(getUserId(principal), id);
        return ResponseEntity.ok(Map.of(
                "message", "Đã xóa danh sách đi chợ"
        ));
    }

    @PostMapping("/lists/{id}/complete")
    public ResponseEntity<Map<String, Object>> completeList(
            Principal principal,
            @PathVariable Long id,
            @RequestBody CompleteGroceryRequest request) {
        GroceryListResponse list = groceryService.completeList(getUserId(principal), id, request);
        return ResponseEntity.ok(Map.of(
                "data", list,
                "message", "Đã hoàn tất danh sách đi chợ"
        ));
    }

    @PutMapping("/lists/{id}")
    public ResponseEntity<Map<String, Object>> updateList(
            Principal principal,
            @PathVariable Long id,
            @Valid @RequestBody GroceryListRequest request) {
        GroceryListResponse list = groceryService.updateList(getUserId(principal), id, request);
        return ResponseEntity.ok(Map.of(
                "data", list,
                "message", "Đã cập nhật danh sách"
        ));
    }

    @DeleteMapping("/lists/{id}/items")
    public ResponseEntity<Map<String, Object>> clearItems(
            Principal principal,
            @PathVariable Long id) {
        groceryService.clearItems(getUserId(principal), id);
        return ResponseEntity.ok(Map.of(
                "message", "Đã xóa tất cả nguyên liệu khỏi danh sách"
        ));
    }

    // ---------- ITEM ENDPOINTS ----------

    @PostMapping("/lists/{id}/items")
    public ResponseEntity<Map<String, Object>> addItem(
            Principal principal,
            @PathVariable Long id,
            @Valid @RequestBody GroceryItemRequest request) {
        GroceryItemResponse item = groceryService.addItem(getUserId(principal), id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "data", item,
                "message", "Đã thêm món vào danh sách"
        ));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<Map<String, Object>> updateItem(
            Principal principal,
            @PathVariable Long itemId,
            @RequestParam Long listId,
            @Valid @RequestBody GroceryItemRequest request) {
        GroceryItemResponse item = groceryService.updateItem(getUserId(principal), listId, itemId, request);
        return ResponseEntity.ok(Map.of(
                "data", item,
                "message", "Đã cập nhật món trong danh sách"
        ));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Map<String, Object>> removeItem(
            Principal principal,
            @PathVariable Long itemId,
            @RequestParam Long listId) {
        groceryService.removeItem(getUserId(principal), listId, itemId);
        return ResponseEntity.ok(Map.of(
                "message", "Đã xóa món khỏi danh sách"
        ));
    }

    @PatchMapping("/items/{itemId}/toggle")
    public ResponseEntity<Map<String, Object>> togglePurchased(
            Principal principal,
            @PathVariable Long itemId,
            @RequestParam Long listId) {
        GroceryItemResponse item = groceryService.togglePurchased(getUserId(principal), listId, itemId);
        return ResponseEntity.ok(Map.of(
                "data", item,
                "message", "Đã cập nhật trạng thái món"
        ));
    }

    // ---------- GENERATE ENDPOINTS ----------

    @PostMapping("/lists/generate-from-pantry")
    public ResponseEntity<Map<String, Object>> generateFromPantry(Principal principal) {
        GroceryListResponse list = groceryService.generateFromPantry(getUserId(principal));
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "data", list,
                "message", "Đã tạo danh sách từ tủ nguyên liệu"
        ));
    }

    @PostMapping("/lists/generate-from-recipe/{recipeId}")
    public ResponseEntity<Map<String, Object>> generateFromRecipe(
            Principal principal,
            @PathVariable Long recipeId,
            @RequestParam(defaultValue = "1") int servings) {
        GroceryListResponse list = groceryService.generateFromRecipe(getUserId(principal), recipeId, servings);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "data", list,
                "message", "Đã tạo danh sách từ công thức"
        ));
    }
}
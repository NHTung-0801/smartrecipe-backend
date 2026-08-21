package com.smartrecipe.smartrecipe_backend.controller;

import com.smartrecipe.smartrecipe_backend.dto.request.JournalRequest;
import com.smartrecipe.smartrecipe_backend.dto.response.ApiResponse;
import com.smartrecipe.smartrecipe_backend.dto.response.JournalResponse;
import com.smartrecipe.smartrecipe_backend.exception.ResourceNotFoundException;
import com.smartrecipe.smartrecipe_backend.repository.UserRepository;
import com.smartrecipe.smartrecipe_backend.service.JournalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/journals")
@RequiredArgsConstructor
public class JournalController {

    private final JournalService journalService;
    private final UserRepository userRepository;

    private Long getUserId(Principal principal) {
        return userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"))
                .getId();
    }

    /** POST /api/v1/journals — Tạo nhật ký mới + Tự động trừ kho (FEFO). */
    @PostMapping
    public ResponseEntity<ApiResponse<JournalResponse>> createJournal(
            @Valid @RequestBody JournalRequest request,
            Principal principal) {
        Long userId = getUserId(principal);
        JournalResponse response = journalService.createJournal(userId, request);
        return new ResponseEntity<>(
                ApiResponse.success(response, "Đã ghi nhận nấu ăn thành công!"),
                HttpStatus.CREATED);
    }

    /** GET /api/v1/journals — Lấy danh sách nhật ký của user, phân trang. */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<JournalResponse>>> getMyJournals(
            Principal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = getUserId(principal);
        Page<JournalResponse> result = journalService.getMyJournals(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(result, "Lấy danh sách nhật ký thành công!"));
    }

    /** GET /api/v1/journals/{id} — Lấy chi tiết 1 nhật ký. */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<JournalResponse>> getJournalById(
            @PathVariable Long id,
            Principal principal) {
        Long userId = getUserId(principal);
        JournalResponse response = journalService.getJournalById(userId, id);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy chi tiết nhật ký thành công!"));
    }

    /** PUT /api/v1/journals/{id} — Cập nhật nhật ký. */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<JournalResponse>> updateJournal(
            @PathVariable Long id,
            @Valid @RequestBody JournalRequest request,
            Principal principal) {
        Long userId = getUserId(principal);
        JournalResponse response = journalService.updateJournal(userId, id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Đã cập nhật đánh giá thành công!"));
    }

    /** DELETE /api/v1/journals/{id} — Xóa nhật ký (không hoàn nguyên kho). */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteJournal(
            @PathVariable Long id,
            Principal principal) {
        Long userId = getUserId(principal);
        journalService.deleteJournal(userId, id);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã xóa nhật ký nấu ăn!"));
    }

    /** POST /api/v1/journals/{id}/image — Upload ảnh cho nhật ký. */
    @PostMapping("/{id}/image")
    public ResponseEntity<ApiResponse<JournalResponse>> uploadJournalImage(
            @PathVariable Long id,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            Principal principal) {
        Long userId = getUserId(principal);
        JournalResponse response = journalService.uploadJournalImage(userId, id, file);
        return ResponseEntity.ok(ApiResponse.success(response, "Đã tải ảnh lên thành công!"));
    }
}

package com.smartrecipe.smartrecipe_backend.controller;

import com.smartrecipe.smartrecipe_backend.dto.request.TagRequest;
import com.smartrecipe.smartrecipe_backend.dto.response.ApiResponse;
import com.smartrecipe.smartrecipe_backend.dto.response.TagResponse;
import com.smartrecipe.smartrecipe_backend.service.TagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TagResponse>>> getAllTags() {
        List<TagResponse> tags = tagService.getAllTags();
        return ResponseEntity.ok(ApiResponse.success(tags, "Lấy danh sách tag thành công!"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TagResponse>> getTagById(@PathVariable Integer id) {
        TagResponse tag = tagService.getTagById(id);
        return ResponseEntity.ok(ApiResponse.success(tag, "Chi tiết tag!"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TagResponse>> createTag(@Valid @RequestBody TagRequest request) {
        TagResponse tag = tagService.createTag(request.getName());
        return new ResponseEntity<>(ApiResponse.success(tag, "Tạo tag thành công!"), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TagResponse>> updateTag(@PathVariable Integer id, @Valid @RequestBody TagRequest request) {
        TagResponse tag = tagService.updateTag(id, request.getName());
        return ResponseEntity.ok(ApiResponse.success(tag, "Cập nhật tag thành công!"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTag(@PathVariable Integer id) {
        tagService.deleteTag(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Xóa tag thành công!"));
    }
}
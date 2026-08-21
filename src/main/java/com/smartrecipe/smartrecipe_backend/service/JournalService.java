package com.smartrecipe.smartrecipe_backend.service;

import com.smartrecipe.smartrecipe_backend.dto.request.JournalRequest;
import com.smartrecipe.smartrecipe_backend.dto.response.JournalResponse;
import org.springframework.data.domain.Page;

public interface JournalService {

    /**
     * Tạo nhật ký nấu ăn mới.
     * Luồng: Trừ kho (FEFO) → Lưu nhật ký → Trả response kèm deductionSummary.
     */
    JournalResponse createJournal(Long userId, JournalRequest request);

    /** Lấy danh sách nhật ký của user, phân trang, mới nhất trước. */
    Page<JournalResponse> getMyJournals(Long userId, int page, int size);

    /** Lấy chi tiết 1 nhật ký (kiểm tra ownership). */
    JournalResponse getJournalById(Long userId, Long journalId);

    /** Xóa nhật ký (không hoàn nguyên kho). */
    void deleteJournal(Long userId, Long journalId);

    /** Cập nhật nhật ký (chỉ cập nhật rating, ghi chú, ảnh, không trừ thêm kho). */
    JournalResponse updateJournal(Long userId, Long journalId, JournalRequest request);

    /** Upload ảnh cho nhật ký */
    JournalResponse uploadJournalImage(Long userId, Long journalId, org.springframework.web.multipart.MultipartFile file);
}

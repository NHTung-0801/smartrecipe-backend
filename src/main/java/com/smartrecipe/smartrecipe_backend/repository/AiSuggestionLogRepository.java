package com.smartrecipe.smartrecipe_backend.repository;

import com.smartrecipe.smartrecipe_backend.entity.AiSuggestionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AiSuggestionLogRepository extends JpaRepository<AiSuggestionLog, Long> {

    /**
     * Đếm số lượt gọi AI của một user từ một thời điểm trở đi.
     * Dùng cho Rate Limiting: đếm từ đầu ngày hôm nay.
     */
    @Query("SELECT COUNT(a) FROM AiSuggestionLog a WHERE a.user.id = :userId AND a.createdAt >= :since")
    long countByUserIdAndCreatedAtAfter(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    /**
     * Lấy danh sách log gợi ý AI của một user, sắp xếp theo thời gian mới nhất.
     */
    List<AiSuggestionLog> findByUserIdOrderByCreatedAtDesc(Long userId);
}

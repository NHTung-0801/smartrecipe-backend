package com.smartrecipe.smartrecipe_backend.repository;

import com.smartrecipe.smartrecipe_backend.entity.AiSuggestionLog;
import com.smartrecipe.smartrecipe_backend.enums.AiSuggestionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AiSuggestionLogRepository extends JpaRepository<AiSuggestionLog, Long> {

    /**
     * Lấy toàn bộ nhật ký AI phân trang theo thời gian mới nhất (Dành cho Admin).
     */
    Page<AiSuggestionLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Lấy nhật ký AI theo loại gợi ý phân trang (Dành cho Admin).
     */
    Page<AiSuggestionLog> findByTypeOrderByCreatedAtDesc(AiSuggestionType type, Pageable pageable);

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

    @Query("SELECT COUNT(a) FROM AiSuggestionLog a WHERE a.savedRecipe IS NOT NULL")
    long countSavedRecipes();

    @Query("SELECT a.type, COUNT(a) FROM AiSuggestionLog a GROUP BY a.type")
    List<Object[]> countGroupByType();

    @Query("SELECT COUNT(a) FROM AiSuggestionLog a WHERE a.createdAt >= :startOfDay")
    long countTodayCalls(@Param("startOfDay") LocalDateTime startOfDay);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE AiSuggestionLog a SET a.savedRecipe = null WHERE a.savedRecipe.id IN :recipeIds")
    void clearSavedRecipeByRecipeIds(@Param("recipeIds") List<Long> recipeIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM AiSuggestionLog a WHERE a.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}

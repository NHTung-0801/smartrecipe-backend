package com.smartrecipe.smartrecipe_backend.repository;

import com.smartrecipe.smartrecipe_backend.entity.CookingJournal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CookingJournalRepository extends JpaRepository<CookingJournal, Long> {

    /** Đếm số lần một công thức được nấu (dùng cho RecipeService). */
    Integer countByRecipeId(Long recipeId);

    /** Lấy danh sách nhật ký của user, phân trang, mới nhất trước. */
    @EntityGraph(attributePaths = {"recipe"})
    Page<CookingJournal> findByUserIdOrderByCookedAtDesc(Long userId, Pageable pageable);

    /** Tìm nhật ký theo id kèm kiểm tra ownership. */
    @EntityGraph(attributePaths = {"recipe"})
    Optional<CookingJournal> findByIdAndUserId(Long id, Long userId);

    /** Đếm số lần user đã nấu một công thức cụ thể. */
    long countByUserIdAndRecipeId(Long userId, Long recipeId);

    /** Đếm tổng số nhật ký của user. */
    long countByUserId(Long userId);

    /** Lấy đánh giá trung bình của user cho một công thức (chỉ tính rating > 0). */
    @org.springframework.data.jpa.repository.Query("SELECT AVG(c.rating) FROM CookingJournal c WHERE c.user.id = :userId AND c.recipe.id = :recipeId AND c.rating > 0")
    Double getAverageRatingByUserIdAndRecipeId(@org.springframework.data.repository.query.Param("userId") Long userId, @org.springframework.data.repository.query.Param("recipeId") Long recipeId);

    /** Lấy đánh giá trung bình toàn hệ thống (chỉ tính rating > 0). */
    @org.springframework.data.jpa.repository.Query("SELECT AVG(c.rating) FROM CookingJournal c WHERE c.rating > 0")
    Double getPlatformAverageRating();
}

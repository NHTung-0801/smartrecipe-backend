package com.smartrecipe.smartrecipe_backend.repository;

import com.smartrecipe.smartrecipe_backend.entity.UserPantry;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PantryRepository extends JpaRepository<UserPantry, Long> {

    /** Lấy toàn bộ tủ của user, join ingredient + aisle, sắp xếp theo aisle name rồi expiry date. */
    @EntityGraph(attributePaths = {"ingredient", "ingredient.aisle"})
    List<UserPantry> findByUserIdOrderByIngredient_Aisle_NameAscExpiryDateAsc(Long userId);

    List<UserPantry> findByUserIdAndIngredientIdOrderByExpiryDateAsc(Long userId, Long ingredientId);

    /** Tìm đúng một mục pantry đồng thời xác nhận ownership của user. */
    Optional<UserPantry> findByIdAndUserId(Long id, Long userId);

    /** Tìm nguyên liệu sắp hết hạn trong khoảng ngày [start, end]. */
    @EntityGraph(attributePaths = {"ingredient", "ingredient.aisle"})
    List<UserPantry> findByUserIdAndExpiryDateBetween(Long userId, LocalDate start, LocalDate end);

    /** Tìm nguyên liệu đã hết hạn (expiryDate < date). */
    List<UserPantry> findByUserIdAndExpiryDateBefore(Long userId, LocalDate date);

    /** Đếm tổng số loại nguyên liệu trong tủ. */
    long countByUserId(Long userId);

    /** Đếm số nguyên liệu sắp hết hạn trong khoảng ngày. */
    long countByUserIdAndExpiryDateBetween(Long userId, LocalDate start, LocalDate end);

    /** Đếm số nguyên liệu đã hết hạn. */
    long countByUserIdAndExpiryDateBefore(Long userId, LocalDate date);

    /** Xóa nguyên liệu đã hết hạn. */
    void deleteByUserIdAndExpiryDateBefore(Long userId, LocalDate date);

    /** Đếm số nguyên liệu dưới ngưỡng lowStockThreshold. */
    @Query("SELECT COUNT(p) FROM UserPantry p WHERE p.user.id = :userId " +
           "AND p.lowStockThreshold IS NOT NULL AND p.quantityAvailable <= p.lowStockThreshold")
    long countLowStockByUserId(@Param("userId") Long userId);

    /** Đếm số nguyên liệu sắp hết hạn hoặc đã hết hạn trên toàn hệ thống trước hoặc trong ngày targetDate. */
    @Query("SELECT COUNT(p) FROM UserPantry p WHERE p.expiryDate IS NOT NULL AND p.expiryDate <= :targetDate")
    long countExpiringSoon(@Param("targetDate") LocalDate targetDate);
}
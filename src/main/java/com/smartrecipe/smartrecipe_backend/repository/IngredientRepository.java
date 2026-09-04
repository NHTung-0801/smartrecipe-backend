package com.smartrecipe.smartrecipe_backend.repository;

import com.smartrecipe.smartrecipe_backend.entity.Ingredient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface IngredientRepository extends JpaRepository<Ingredient, Long> {

    List<Ingredient> findByNameContainingIgnoreCase(String name);

    Page<Ingredient> findByNameContainingIgnoreCase(String name, Pageable pageable);
    
    Optional<Ingredient> findFirstByNameIgnoreCase(String name);

    List<Ingredient> findByAisleId(Integer aisleId);

    // Admin: nguyên liệu chờ duyệt (calories=0, loại trừ gia vị như muối)
    @Query("SELECT i FROM Ingredient i WHERE i.caloriesPer100g = 0 " +
           "AND LOWER(i.name) NOT LIKE '%muối%' " +
           "AND LOWER(i.name) NOT LIKE '%salt%'")
    Page<Ingredient> findPendingReview(Pageable pageable);

    // Admin: tổng số nguyên liệu chờ duyệt
    @Query("SELECT COUNT(i) FROM Ingredient i WHERE i.caloriesPer100g = 0 " +
           "AND LOWER(i.name) NOT LIKE '%muối%' " +
           "AND LOWER(i.name) NOT LIKE '%salt%'")
    long countPendingReview();
}
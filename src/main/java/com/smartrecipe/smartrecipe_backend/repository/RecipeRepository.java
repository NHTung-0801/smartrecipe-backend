package com.smartrecipe.smartrecipe_backend.repository;

import com.smartrecipe.smartrecipe_backend.entity.Recipe;
import com.smartrecipe.smartrecipe_backend.entity.User;
import com.smartrecipe.smartrecipe_backend.enums.RecipeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    Page<Recipe> findByAuthorAndStatusNot(User author, RecipeStatus status, Pageable pageable);

    Page<Recipe> findByStatus(RecipeStatus status, Pageable pageable);

    long countByAuthorIdAndStatus(Long authorId, RecipeStatus status);

    long countByAuthorIdAndStatusNot(Long authorId, RecipeStatus status);

    List<Recipe> findByClonedFromId(Long clonedFromId);

    @Query("SELECT r FROM Recipe r WHERE r.status = 'PUBLIC' AND " +
           "(:keyword IS NULL OR LOWER(r.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(r.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Recipe> searchPublicRecipes(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT r FROM Recipe r WHERE r.author.id = :userId ORDER BY r.createdAt DESC")
    List<Recipe> findRecentByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT r FROM Recipe r WHERE r.status = 'PUBLIC' AND r.author.id = :userId")
    Page<Recipe> findPublicByAuthorId(@Param("userId") Long userId, Pageable pageable);
}
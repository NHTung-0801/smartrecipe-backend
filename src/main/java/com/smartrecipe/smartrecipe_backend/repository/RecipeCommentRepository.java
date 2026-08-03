package com.smartrecipe.smartrecipe_backend.repository;

import com.smartrecipe.smartrecipe_backend.entity.RecipeComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecipeCommentRepository extends JpaRepository<RecipeComment, Long> {

    // Lấy tất cả bình luận gốc (parent is null) của một recipe, sắp xếp mới nhất trước
    List<RecipeComment> findByRecipeIdAndParentIsNullOrderByCreatedAtDesc(Long recipeId);

    long countByRecipeId(Long recipeId);

    void deleteByRecipeId(Long recipeId);
}

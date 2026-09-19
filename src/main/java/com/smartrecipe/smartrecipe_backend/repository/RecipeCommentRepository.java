package com.smartrecipe.smartrecipe_backend.repository;

import com.smartrecipe.smartrecipe_backend.entity.RecipeComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecipeCommentRepository extends JpaRepository<RecipeComment, Long> {

    // Lấy tất cả bình luận gốc (parent is null) của một recipe, sắp xếp mới nhất trước
    List<RecipeComment> findByRecipeIdAndParentIsNullOrderByCreatedAtDesc(Long recipeId);

    long countByRecipeId(Long recipeId);

    void deleteByRecipeId(Long recipeId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE RecipeComment rc SET rc.parent = null WHERE rc.recipe.id IN :recipeIds")
    void clearParentByRecipeIds(@Param("recipeIds") List<Long> recipeIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE RecipeComment rc SET rc.parent = null WHERE rc.parent.id IN (SELECT p.id FROM RecipeComment p WHERE p.user.id = :userId)")
    void clearParentByUserId(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM RecipeComment rc WHERE rc.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}

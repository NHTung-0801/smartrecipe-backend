package com.smartrecipe.smartrecipe_backend.repository;

import com.smartrecipe.smartrecipe_backend.entity.RecipeLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecipeLikeRepository extends JpaRepository<RecipeLike, Long> {

    Optional<RecipeLike> findByUserIdAndRecipeId(Long userId, Long recipeId);

    boolean existsByUserIdAndRecipeId(Long userId, Long recipeId);

    long countByRecipeId(Long recipeId);

    void deleteByUserIdAndRecipeId(Long userId, Long recipeId);

    @Query("SELECT rl.recipe.id FROM RecipeLike rl WHERE rl.user.id = :userId")
    List<Long> findLikedRecipeIdsByUserId(@Param("userId") Long userId);
}
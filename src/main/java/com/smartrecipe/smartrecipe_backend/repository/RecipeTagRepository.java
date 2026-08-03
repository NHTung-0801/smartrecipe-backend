package com.smartrecipe.smartrecipe_backend.repository;

import com.smartrecipe.smartrecipe_backend.entity.RecipeTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecipeTagRepository extends JpaRepository<RecipeTag, Long> {

    List<RecipeTag> findByRecipeId(Long recipeId);

    void deleteByRecipeId(Long recipeId);

    boolean existsByRecipeIdAndTagId(Long recipeId, Integer tagId);
}
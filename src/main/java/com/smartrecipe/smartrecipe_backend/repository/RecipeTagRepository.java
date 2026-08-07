package com.smartrecipe.smartrecipe_backend.repository;

import com.smartrecipe.smartrecipe_backend.entity.RecipeTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;

@Repository
public interface RecipeTagRepository extends JpaRepository<RecipeTag, Long> {

    List<RecipeTag> findByRecipeId(Long recipeId);

    void deleteByRecipeId(Long recipeId);

    @Modifying
    @Query("DELETE FROM RecipeTag rt WHERE rt.tag.id = :tagId")
    void deleteByTagId(@Param("tagId") Integer tagId);

    boolean existsByRecipeIdAndTagId(Long recipeId, Integer tagId);
}
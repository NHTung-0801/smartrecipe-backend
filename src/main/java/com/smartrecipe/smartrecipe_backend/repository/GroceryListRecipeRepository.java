package com.smartrecipe.smartrecipe_backend.repository;

import com.smartrecipe.smartrecipe_backend.entity.GroceryListRecipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface GroceryListRecipeRepository extends JpaRepository<GroceryListRecipe, Long> {

    List<GroceryListRecipe> findByGroceryListId(Long groceryListId);

    @Modifying
    @Transactional
    void deleteByGroceryListId(Long groceryListId);
}
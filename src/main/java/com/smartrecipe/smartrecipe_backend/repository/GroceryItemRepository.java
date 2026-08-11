package com.smartrecipe.smartrecipe_backend.repository;

import com.smartrecipe.smartrecipe_backend.entity.GroceryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroceryItemRepository extends JpaRepository<GroceryItem, Long> {

    List<GroceryItem> findByGroceryListIdOrderByIngredient_Aisle_NameAsc(Long groceryListId);

    Optional<GroceryItem> findByIdAndGroceryListId(Long itemId, Long groceryListId);

    @Modifying
    @Query("DELETE FROM GroceryItem gi WHERE gi.groceryList.id = :listId")
    void deleteByGroceryListId(@Param("listId") Long listId);

    Optional<GroceryItem> findByGroceryListIdAndIngredientId(Long groceryListId, Long ingredientId);
}
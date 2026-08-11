package com.smartrecipe.smartrecipe_backend.repository;

import com.smartrecipe.smartrecipe_backend.entity.GroceryList;
import com.smartrecipe.smartrecipe_backend.enums.GroceryListStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroceryListRepository extends JpaRepository<GroceryList, Long> {
    List<GroceryList> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<GroceryList> findByIdAndUserId(Long id, Long userId);
    Optional<GroceryList> findFirstByUserIdAndStatusOrderByCreatedAtDesc(
            Long userId, GroceryListStatus status);
}

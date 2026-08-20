package com.smartrecipe.smartrecipe_backend.repository;

import com.smartrecipe.smartrecipe_backend.entity.CookingJournal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CookingJournalRepository extends JpaRepository<CookingJournal, Long> {
    Integer countByRecipeId(Long recipeId);
}

package com.smartrecipe.smartrecipe_backend.service.impl;

import com.smartrecipe.smartrecipe_backend.dto.request.JournalRequest;
import com.smartrecipe.smartrecipe_backend.dto.response.JournalResponse;
import com.smartrecipe.smartrecipe_backend.entity.CookingJournal;
import com.smartrecipe.smartrecipe_backend.entity.Recipe;
import com.smartrecipe.smartrecipe_backend.entity.User;
import com.smartrecipe.smartrecipe_backend.exception.ResourceNotFoundException;
import com.smartrecipe.smartrecipe_backend.repository.CookingJournalRepository;
import com.smartrecipe.smartrecipe_backend.repository.RecipeRepository;
import com.smartrecipe.smartrecipe_backend.repository.UserRepository;
import com.smartrecipe.smartrecipe_backend.service.CloudinaryService;
import com.smartrecipe.smartrecipe_backend.service.JournalService;
import com.smartrecipe.smartrecipe_backend.service.PantryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class JournalServiceImpl implements JournalService {

    private final CookingJournalRepository journalRepository;
    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;
    private final PantryService pantryService;
    private final CloudinaryService cloudinaryService;

    @Override
    public JournalResponse createJournal(Long userId, JournalRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId));

        Recipe recipe = recipeRepository.findById(request.getRecipeId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công thức với ID: " + request.getRecipeId()));

        // Bước 1 — Trừ kho FEFO trước khi lưu nhật ký
        List<JournalResponse.DeductionDetail> deductions =
                pantryService.deductIngredientsForRecipe(userId, recipe.getId(), request.getActualServings());

        // Bước 2 — Lưu nhật ký
        CookingJournal journal = CookingJournal.builder()
                .user(user)
                .recipe(recipe)
                .actualServings(request.getActualServings())
                .rating(request.getRating())
                .iterationNotes(request.getIterationNotes())
                .imageUrl(request.getImageUrl())
                .build();

        CookingJournal saved = journalRepository.save(journal);

        JournalResponse response = mapToResponse(saved);
        response.setDeductionSummary(deductions);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JournalResponse> getMyJournals(Long userId, int page, int size) {
        return journalRepository.findByUserIdOrderByCookedAtDesc(userId, PageRequest.of(page, size))
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public JournalResponse getJournalById(Long userId, Long journalId) {
        CookingJournal journal = journalRepository.findByIdAndUserId(journalId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhật ký với ID: " + journalId));
        return mapToResponse(journal);
    }

    @Override
    public void deleteJournal(Long userId, Long journalId) {
        CookingJournal journal = journalRepository.findByIdAndUserId(journalId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhật ký với ID: " + journalId));
        journalRepository.delete(journal);
    }

    @Override
    public JournalResponse updateJournal(Long userId, Long journalId, JournalRequest request) {
        CookingJournal journal = journalRepository.findByIdAndUserId(journalId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhật ký với ID: " + journalId));
        
        // Chỉ cho phép cập nhật rating, iterationNotes, imageUrl
        if (request.getRating() != null) {
            journal.setRating(request.getRating());
        }
        if (request.getIterationNotes() != null) {
            journal.setIterationNotes(request.getIterationNotes());
        }
        if (request.getImageUrl() != null) {
            journal.setImageUrl(request.getImageUrl());
        }

        CookingJournal saved = journalRepository.save(journal);
        return mapToResponse(saved);
    }

    @Override
    public JournalResponse uploadJournalImage(Long userId, Long journalId, MultipartFile file) {
        CookingJournal journal = journalRepository.findByIdAndUserId(journalId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhật ký với ID: " + journalId));
        
        String imageUrl = cloudinaryService.uploadImage(file, "smartrecipe/journals");
        journal.setImageUrl(imageUrl);
        CookingJournal saved = journalRepository.save(journal);
        return mapToResponse(saved);
    }

    // ========== Helper ==========

    private JournalResponse mapToResponse(CookingJournal journal) {
        JournalResponse.RecipeSummaryInfo recipeInfo = null;
        Double avgRating = null;
        if (journal.getRecipe() != null) {
            Recipe r = journal.getRecipe();
            recipeInfo = JournalResponse.RecipeSummaryInfo.builder()
                    .id(r.getId())
                    .title(r.getTitle())
                    .imageUrl(r.getImageUrl())
                    .baseServings(r.getBaseServings())
                    .build();
            
            if (journal.getUser() != null) {
                avgRating = journalRepository.getAverageRatingByUserIdAndRecipeId(journal.getUser().getId(), r.getId());
            }
        }

        return JournalResponse.builder()
                .id(journal.getId())
                .recipe(recipeInfo)
                .cookedAt(journal.getCookedAt())
                .actualServings(journal.getActualServings())
                .rating(journal.getRating())
                .iterationNotes(journal.getIterationNotes())
                .imageUrl(journal.getImageUrl())
                .averageRating(avgRating)
                .build();
    }
}

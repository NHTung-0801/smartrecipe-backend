package com.smartrecipe.smartrecipe_backend.service.impl;

import com.smartrecipe.smartrecipe_backend.dto.request.PantryRequest;
import com.smartrecipe.smartrecipe_backend.dto.response.JournalResponse;
import com.smartrecipe.smartrecipe_backend.dto.response.PantryResponse;
import com.smartrecipe.smartrecipe_backend.entity.*;
import com.smartrecipe.smartrecipe_backend.exception.BadRequestException;
import com.smartrecipe.smartrecipe_backend.exception.ResourceNotFoundException;
import com.smartrecipe.smartrecipe_backend.repository.IngredientRepository;
import com.smartrecipe.smartrecipe_backend.repository.PantryRepository;
import com.smartrecipe.smartrecipe_backend.repository.RecipeRepository;
import com.smartrecipe.smartrecipe_backend.repository.UserRepository;
import com.smartrecipe.smartrecipe_backend.service.UnitNormalizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PantryServiceImplTest {

    @Mock PantryRepository pantryRepository;
    @Mock IngredientRepository ingredientRepository;
    @Mock UserRepository userRepository;
    @Mock RecipeRepository recipeRepository;
    @Mock UnitNormalizationService unitNormalizationService;

    private PantryServiceImpl service;
    private User user;
    private Ingredient ingredient;

    @BeforeEach
    void setUp() {
        service = new PantryServiceImpl(pantryRepository, ingredientRepository,
                userRepository, recipeRepository, unitNormalizationService);
        user = User.builder().id(7L).username("owner").build();
        ingredient = Ingredient.builder().id(11L).name("Gạo").baseUnit("g").build();
    }

    @Test
    void addMergesOnlySameIngredientAndSameExpiryLot() {
        LocalDate expiry = LocalDate.of(2026, 9, 1);
        UserPantry existing = lot(20L, expiry, "500");
        PantryRequest request = request(expiry, "1", "kg");
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(ingredientRepository.findById(11L)).thenReturn(Optional.of(ingredient));
        when(unitNormalizationService.toBaseUnit(new BigDecimal("1"), "kg", ingredient))
                .thenReturn(new BigDecimal("1000"));
        when(pantryRepository.findByUserIdAndIngredientIdOrderByExpiryDateAsc(7L, 11L))
                .thenReturn(List.of(existing));
        when(pantryRepository.save(existing)).thenReturn(existing);

        PantryResponse response = service.addOrUpdateItem(7L, request);

        assertThat(response.getQuantityAvailable()).isEqualByComparingTo("1500");
        verify(pantryRepository).save(existing);
    }

    @Test
    void addCreatesSeparateLotWhenExpiryDiffers() {
        LocalDate expiry = LocalDate.of(2026, 9, 2);
        PantryRequest request = request(expiry, "250", "g");
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(ingredientRepository.findById(11L)).thenReturn(Optional.of(ingredient));
        when(unitNormalizationService.toBaseUnit(new BigDecimal("250"), "g", ingredient))
                .thenReturn(new BigDecimal("250"));
        when(pantryRepository.findByUserIdAndIngredientIdOrderByExpiryDateAsc(7L, 11L))
                .thenReturn(List.of());
        when(pantryRepository.save(any(UserPantry.class))).thenAnswer(invocation -> {
            UserPantry saved = invocation.getArgument(0);
            saved.setId(21L);
            return saved;
        });

        PantryResponse response = service.addOrUpdateItem(7L, request);

        assertThat(response.getId()).isEqualTo(21L);
        assertThat(response.getExpiryDate()).isEqualTo(expiry);
        assertThat(response.getQuantityAvailable()).isEqualByComparingTo("250");
    }

    @Test
    void updateRejectsPantryLotOwnedByAnotherUser() {
        when(pantryRepository.findByIdAndUserId(99L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateItem(7L, 99L,
                request(LocalDate.of(2026, 9, 1), "1", "kg")))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(pantryRepository, never()).save(any());
    }

    @Test
    void deleteRejectsPantryLotOwnedByAnotherUser() {
        when(pantryRepository.findByIdAndUserId(99L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.removeItem(7L, 99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(pantryRepository, never()).delete(any());
    }

    @Test
    void lowStockUsesTotalQuantityAcrossLots() {
        UserPantry first = lot(1L, LocalDate.of(2026, 9, 1), "60");
        UserPantry second = lot(2L, LocalDate.of(2026, 9, 2), "60");
        first.setLowStockThreshold(new BigDecimal("100"));
        second.setLowStockThreshold(new BigDecimal("100"));
        when(pantryRepository.findByUserIdOrderByIngredient_Aisle_NameAscExpiryDateAsc(7L))
                .thenReturn(List.of(first, second));

        Map<String, List<PantryResponse>> result = service.getMyPantry(7L, "LOW_STOCK");

        assertThat(result).isEmpty();
    }

    // ========== FEFO Deduction Tests (Task 2.1) ==========

    @Nested
    @DisplayName("deductIngredientsForRecipe — FEFO Deduction")
    class FEFODeductionTests {

        private Ingredient egg;
        private Recipe recipe;

        @BeforeEach
        void setUpRecipe() {
            egg = Ingredient.builder().id(100L).name("Trứng gà").baseUnit("quả").build();

            RecipeIngredient ri = RecipeIngredient.builder()
                    .ingredient(egg)
                    .amount(new BigDecimal("3"))   // 3 quả cho 2 người
                    .unit("quả")
                    .build();

            recipe = Recipe.builder()
                    .id(1L)
                    .title("Trứng chiên")
                    .baseServings(2)
                    .ingredients(new ArrayList<>(List.of(ri)))
                    .build();
        }

        @Test
        @DisplayName("Case 1: Trừ vừa đủ từ 1 lô duy nhất")
        void deductExactFromSingleLot() {
            when(recipeRepository.findById(1L)).thenReturn(Optional.of(recipe));
            when(unitNormalizationService.toBaseUnit(any(BigDecimal.class), eq("quả"), eq(egg)))
                    .thenAnswer(inv -> inv.getArgument(0)); // quả → quả, no conversion
            UserPantry lot1 = UserPantry.builder().id(10L).user(user).ingredient(egg)
                    .quantityAvailable(new BigDecimal("5")).expiryDate(LocalDate.of(2026, 9, 1)).build();
            when(pantryRepository.findByUserIdAndIngredientIdOrderByExpiryDateAsc(7L, 100L))
                    .thenReturn(new ArrayList<>(List.of(lot1)));
            when(pantryRepository.save(lot1)).thenReturn(lot1);

            // Nấu cho 2 người (= baseServings) → cần 3 quả
            List<JournalResponse.DeductionDetail> result = service.deductIngredientsForRecipe(7L, 1L, 2);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getIngredientName()).isEqualTo("Trứng gà");
            assertThat(result.get(0).getDeductedAmount()).isEqualTo(3.0);
            assertThat(lot1.getQuantityAvailable()).isEqualByComparingTo("2"); // 5 - 3 = 2
            verify(pantryRepository).save(lot1);
            verify(pantryRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Case 2: Trừ vượt qua nhiều lô (FEFO)")
        void deductAcrossMultipleLots() {
            when(recipeRepository.findById(1L)).thenReturn(Optional.of(recipe));
            when(unitNormalizationService.toBaseUnit(any(BigDecimal.class), eq("quả"), eq(egg)))
                    .thenAnswer(inv -> inv.getArgument(0));
            // Lô 1: hết hạn ngày mai, chỉ còn 1 quả
            UserPantry lot1 = UserPantry.builder().id(10L).user(user).ingredient(egg)
                    .quantityAvailable(new BigDecimal("1")).expiryDate(LocalDate.of(2026, 8, 22)).build();
            // Lô 2: hết hạn tuần sau, còn 10 quả
            UserPantry lot2 = UserPantry.builder().id(11L).user(user).ingredient(egg)
                    .quantityAvailable(new BigDecimal("10")).expiryDate(LocalDate.of(2026, 8, 28)).build();
            when(pantryRepository.findByUserIdAndIngredientIdOrderByExpiryDateAsc(7L, 100L))
                    .thenReturn(new ArrayList<>(List.of(lot1, lot2)));
            when(pantryRepository.save(lot2)).thenReturn(lot2);

            // Nấu cho 2 người → cần 3 quả
            List<JournalResponse.DeductionDetail> result = service.deductIngredientsForRecipe(7L, 1L, 2);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getDeductedAmount()).isEqualTo(3.0); // 1 + 2
            verify(pantryRepository).delete(lot1);  // Lô 1 bị xóa (hết)
            verify(pantryRepository).save(lot2);    // Lô 2 còn 8 quả
            assertThat(lot2.getQuantityAvailable()).isEqualByComparingTo("8");
        }

        @Test
        @DisplayName("Case 3: Kho không đủ → trừ về 0, không exception")
        void deductInsufficientStock_noException() {
            when(recipeRepository.findById(1L)).thenReturn(Optional.of(recipe));
            when(unitNormalizationService.toBaseUnit(any(BigDecimal.class), eq("quả"), eq(egg)))
                    .thenAnswer(inv -> inv.getArgument(0));
            // Chỉ còn 1 quả nhưng cần 3
            UserPantry lot1 = UserPantry.builder().id(10L).user(user).ingredient(egg)
                    .quantityAvailable(new BigDecimal("1")).expiryDate(LocalDate.of(2026, 9, 1)).build();
            when(pantryRepository.findByUserIdAndIngredientIdOrderByExpiryDateAsc(7L, 100L))
                    .thenReturn(new ArrayList<>(List.of(lot1)));

            List<JournalResponse.DeductionDetail> result = service.deductIngredientsForRecipe(7L, 1L, 2);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getDeductedAmount()).isEqualTo(1.0); // chỉ trừ được 1
            verify(pantryRepository).delete(lot1); // lô bị xóa vì hết
        }

        @Test
        @DisplayName("Case 4: Nguyên liệu không có trong kho → skip, không exception")
        void deductMissingIngredient_skip() {
            when(recipeRepository.findById(1L)).thenReturn(Optional.of(recipe));
            when(unitNormalizationService.toBaseUnit(any(BigDecimal.class), eq("quả"), eq(egg)))
                    .thenAnswer(inv -> inv.getArgument(0));
            // Kho rỗng cho nguyên liệu này
            when(pantryRepository.findByUserIdAndIngredientIdOrderByExpiryDateAsc(7L, 100L))
                    .thenReturn(new ArrayList<>());

            List<JournalResponse.DeductionDetail> result = service.deductIngredientsForRecipe(7L, 1L, 2);

            assertThat(result).isEmpty(); // không trừ gì, không lỗi
            verify(pantryRepository, never()).delete(any());
            verify(pantryRepository, never()).save(any());
        }

        @Test
        @DisplayName("Case 5: Đơn vị không quy đổi được → skip, không exception")
        void deductUnconvertibleUnit_skip() {
            when(recipeRepository.findById(1L)).thenReturn(Optional.of(recipe));
            when(unitNormalizationService.toBaseUnit(any(BigDecimal.class), eq("quả"), eq(egg)))
                    .thenThrow(new BadRequestException("Không thể quy đổi 'quả' sang 'g'"));

            List<JournalResponse.DeductionDetail> result = service.deductIngredientsForRecipe(7L, 1L, 2);

            assertThat(result).isEmpty(); // bỏ qua, không lỗi
            verify(pantryRepository, never()).delete(any());
            verify(pantryRepository, never()).save(any());
        }
    }

    // ========== Helpers ==========

    private PantryRequest request(LocalDate expiry, String quantity, String unit) {
        PantryRequest request = new PantryRequest();
        request.setIngredientId(11L);
        request.setQuantityAvailable(new BigDecimal(quantity));
        request.setExpiryDate(expiry);
        request.setUnit(unit);
        return request;
    }

    private UserPantry lot(Long id, LocalDate expiry, String quantity) {
        return UserPantry.builder()
                .id(id)
                .user(user)
                .ingredient(ingredient)
                .quantityAvailable(new BigDecimal(quantity))
                .expiryDate(expiry)
                .build();
    }
}

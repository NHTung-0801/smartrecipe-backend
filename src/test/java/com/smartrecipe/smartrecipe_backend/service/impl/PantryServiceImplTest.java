package com.smartrecipe.smartrecipe_backend.service.impl;

import com.smartrecipe.smartrecipe_backend.dto.request.PantryRequest;
import com.smartrecipe.smartrecipe_backend.dto.response.PantryResponse;
import com.smartrecipe.smartrecipe_backend.entity.Ingredient;
import com.smartrecipe.smartrecipe_backend.entity.User;
import com.smartrecipe.smartrecipe_backend.entity.UserPantry;
import com.smartrecipe.smartrecipe_backend.exception.ResourceNotFoundException;
import com.smartrecipe.smartrecipe_backend.repository.IngredientRepository;
import com.smartrecipe.smartrecipe_backend.repository.PantryRepository;
import com.smartrecipe.smartrecipe_backend.repository.UserRepository;
import com.smartrecipe.smartrecipe_backend.service.UnitNormalizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PantryServiceImplTest {

    @Mock PantryRepository pantryRepository;
    @Mock IngredientRepository ingredientRepository;
    @Mock UserRepository userRepository;
    @Mock UnitNormalizationService unitNormalizationService;

    private PantryServiceImpl service;
    private User user;
    private Ingredient ingredient;

    @BeforeEach
    void setUp() {
        service = new PantryServiceImpl(pantryRepository, ingredientRepository, userRepository, unitNormalizationService);
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
        when(pantryRepository.findByUserIdAndIngredientIdAndExpiryDate(7L, 11L, expiry))
                .thenReturn(Optional.of(existing));
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
        when(pantryRepository.findByUserIdAndIngredientIdAndExpiryDate(7L, 11L, expiry))
                .thenReturn(Optional.empty());
        when(pantryRepository.findByUserIdAndIngredientIdOrderByExpiryDateAsc(7L, 11L)).thenReturn(List.of());
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
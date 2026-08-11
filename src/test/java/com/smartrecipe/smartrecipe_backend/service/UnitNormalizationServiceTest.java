package com.smartrecipe.smartrecipe_backend.service;

import com.smartrecipe.smartrecipe_backend.entity.Ingredient;
import com.smartrecipe.smartrecipe_backend.entity.UnitConversion;
import com.smartrecipe.smartrecipe_backend.exception.BadRequestException;
import com.smartrecipe.smartrecipe_backend.repository.UnitConversionRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UnitNormalizationServiceTest {

    private final UnitConversionRepository repository = mock(UnitConversionRepository.class);
    private final UnitNormalizationService service = new UnitNormalizationService(repository);

    @Test
    void convertsKilogramsToGramBaseUnit() {
        Ingredient ingredient = ingredient(1L, "Gạo", "g");
        when(repository.findByIngredientIdOrIngredientIsNull(1L)).thenReturn(List.of(
                conversion("kg", "g", "1000")
        ));

        BigDecimal result = service.toBaseUnit(new BigDecimal("1.5"), "kg", ingredient);

        assertThat(result).isEqualByComparingTo("1500");
    }

    @Test
    void convertsVietnameseLiterAliasToMilliliters() {
        Ingredient ingredient = ingredient(2L, "Sữa", "ml");
        when(repository.findByIngredientIdOrIngredientIsNull(2L)).thenReturn(List.of(
                conversion("l", "ml", "1000")
        ));

        BigDecimal result = service.toBaseUnit(new BigDecimal("2"), "lít", ingredient);

        assertThat(result).isEqualByComparingTo("2000");
    }

    @Test
    void supportsReverseConversionEdges() {
        Ingredient ingredient = ingredient(3L, "Bột", "kg");
        when(repository.findByIngredientIdOrIngredientIsNull(3L)).thenReturn(List.of(
                conversion("kg", "g", "1000")
        ));

        BigDecimal result = service.toBaseUnit(new BigDecimal("500"), "g (gram)", ingredient);

        assertThat(result).isEqualByComparingTo("0.5");
    }

    @Test
    void rejectsUnitsWithoutConversionPath() {
        Ingredient ingredient = ingredient(4L, "Trứng", "quả");
        when(repository.findByIngredientIdOrIngredientIsNull(4L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.toBaseUnit(BigDecimal.ONE, "kg", ingredient))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Không thể quy đổi đơn vị");
    }

    private Ingredient ingredient(Long id, String name, String baseUnit) {
        return Ingredient.builder().id(id).name(name).baseUnit(baseUnit).build();
    }

    private UnitConversion conversion(String from, String to, String multiplier) {
        return UnitConversion.builder()
                .fromUnit(from)
                .toUnit(to)
                .multiplier(new BigDecimal(multiplier))
                .build();
    }
}
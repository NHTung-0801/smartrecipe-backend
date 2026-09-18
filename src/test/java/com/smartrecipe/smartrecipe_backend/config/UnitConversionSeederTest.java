package com.smartrecipe.smartrecipe_backend.config;

import com.smartrecipe.smartrecipe_backend.entity.UnitConversion;
import com.smartrecipe.smartrecipe_backend.repository.UnitConversionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UnitConversionSeederTest {

    @Mock
    private UnitConversionRepository unitConversionRepository;

    @Mock
    private ApplicationArguments args;

    @InjectMocks
    private UnitConversionSeeder seeder;

    @Test
    @DisplayName("Khi tất cả các quy tắc đã tồn tại trong DB, seeder bỏ qua không insert thêm")
    void testRun_whenAllRulesExist_doesNotInsert() {
        when(unitConversionRepository.existsByFromUnitAndToUnitAndIngredientIsNull(anyString(), anyString()))
                .thenReturn(true);

        seeder.run(args);

        verify(unitConversionRepository, never()).save(any(UnitConversion.class));
    }

    @Test
    @DisplayName("Khi chưa có quy tắc nào trong DB, seeder insert đầy đủ các quy tắc toàn cục")
    void testRun_whenRulesDoNotExist_insertsRules() {
        when(unitConversionRepository.existsByFromUnitAndToUnitAndIngredientIsNull(anyString(), anyString()))
                .thenReturn(false);

        seeder.run(args);

        verify(unitConversionRepository, atLeastOnce()).save(any(UnitConversion.class));
    }
}

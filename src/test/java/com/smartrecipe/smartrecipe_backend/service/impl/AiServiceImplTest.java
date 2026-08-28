package com.smartrecipe.smartrecipe_backend.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartrecipe.smartrecipe_backend.config.GeminiClient;
import com.smartrecipe.smartrecipe_backend.config.GeminiConfig;
import com.smartrecipe.smartrecipe_backend.dto.request.RecipeRequest;
import com.smartrecipe.smartrecipe_backend.dto.response.AiHistoryResponse;
import com.smartrecipe.smartrecipe_backend.dto.response.AiRemainingResponse;
import com.smartrecipe.smartrecipe_backend.dto.response.AiSuggestResponse;
import com.smartrecipe.smartrecipe_backend.dto.response.RecipeResponse;
import com.smartrecipe.smartrecipe_backend.entity.AiSuggestionLog;
import com.smartrecipe.smartrecipe_backend.entity.Ingredient;
import com.smartrecipe.smartrecipe_backend.entity.Recipe;
import com.smartrecipe.smartrecipe_backend.entity.User;
import com.smartrecipe.smartrecipe_backend.enums.AiSuggestionType;
import com.smartrecipe.smartrecipe_backend.exception.AiServiceException;
import com.smartrecipe.smartrecipe_backend.exception.BadRequestException;
import com.smartrecipe.smartrecipe_backend.exception.RateLimitExceededException;
import com.smartrecipe.smartrecipe_backend.repository.AiSuggestionLogRepository;
import com.smartrecipe.smartrecipe_backend.repository.IngredientRepository;
import com.smartrecipe.smartrecipe_backend.repository.UserRepository;
import com.smartrecipe.smartrecipe_backend.service.PantryService;
import com.smartrecipe.smartrecipe_backend.service.RecipeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiServiceImplTest {

    @Mock GeminiClient geminiClient;
    @Mock GeminiConfig geminiConfig;
    @Mock PantryService pantryService;
    @Mock AiSuggestionLogRepository aiLogRepository;
    @Mock UserRepository userRepository;
    @Mock IngredientRepository ingredientRepository;
    @Mock RecipeService recipeService;

    private AiServiceImpl service;
    private User user;

    /** JSON tối giản đúng cấu trúc mà SYSTEM_PROMPT yêu cầu Gemini trả về. */
    private static String aiJson(String ingredientName, String unit) {
        return """
                {
                  "title": "Gà xào sả ớt",
                  "description": "Món mặn đưa cơm",
                  "baseServings": 2,
                  "prepTime": 10,
                  "cookTime": 15,
                  "difficulty": "EASY",
                  "ingredients": [
                    { "ingredientName": "%s", "amount": 200, "unit": "%s" }
                  ],
                  "steps": [
                    { "stepNumber": 1, "instruction": "Xào chín" }
                  ]
                }
                """.formatted(ingredientName, unit);
    }

    private static Ingredient ingredient(Long id, String name, String baseUnit) {
        return Ingredient.builder()
                .id(id).name(name).baseUnit(baseUnit)
                .caloriesPer100g(new BigDecimal("100"))
                .protein(BigDecimal.ONE).fat(BigDecimal.ONE).carbs(BigDecimal.ONE)
                .build();
    }

    @BeforeEach
    void setUp() {
        service = new AiServiceImpl(geminiClient, geminiConfig, pantryService,
                aiLogRepository, userRepository, new ObjectMapper(),
                ingredientRepository, recipeService);
        user = User.builder().id(7L).username("owner").build();
    }

    @Nested
    @DisplayName("Rate limit")
    class RateLimit {

        @Test
        void blocksWhenDailyLimitReached() {
            when(geminiConfig.getDailyLimit()).thenReturn(10);
            when(aiLogRepository.countByUserIdAndCreatedAtAfter(eq(7L), any(LocalDateTime.class)))
                    .thenReturn(10L);

            assertThatThrownBy(() -> service.suggestFromInput(7L, List.of("gà")))
                    .isInstanceOf(RateLimitExceededException.class)
                    .hasMessageContaining("10 lượt");

            // Không được gọi Gemini khi đã hết lượt — gọi là tốn quota thật
            verifyNoInteractions(geminiClient);
        }

        @Test
        void remainingNeverGoesNegativeWhenLimitLowered() {
            // Kịch bản: user đã dùng 12 lượt rồi admin hạ giới hạn xuống 10
            when(geminiConfig.getDailyLimit()).thenReturn(10);
            when(aiLogRepository.countByUserIdAndCreatedAtAfter(eq(7L), any(LocalDateTime.class)))
                    .thenReturn(12L);

            AiRemainingResponse result = service.getRemaining(7L);

            assertThat(result.getUsed()).isEqualTo(12);
            assertThat(result.getDailyLimit()).isEqualTo(10);
            assertThat(result.getRemaining()).isZero();
        }

        @Test
        void remainingCountsDownFromLimit() {
            when(geminiConfig.getDailyLimit()).thenReturn(10);
            when(aiLogRepository.countByUserIdAndCreatedAtAfter(eq(7L), any(LocalDateTime.class)))
                    .thenReturn(3L);

            assertThat(service.getRemaining(7L).getRemaining()).isEqualTo(7);
        }
    }

    @Nested
    @DisplayName("suggestFromInput")
    class SuggestFromInput {

        @Test
        void rejectsEmptyIngredientList() {
            when(geminiConfig.getDailyLimit()).thenReturn(10);
            when(aiLogRepository.countByUserIdAndCreatedAtAfter(eq(7L), any(LocalDateTime.class)))
                    .thenReturn(0L);

            assertThatThrownBy(() -> service.suggestFromInput(7L, List.of()))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        void parsesResponseAndSavesLog() {
            when(geminiConfig.getDailyLimit()).thenReturn(10);
            when(aiLogRepository.countByUserIdAndCreatedAtAfter(eq(7L), any(LocalDateTime.class)))
                    .thenReturn(0L);
            when(geminiClient.generate(anyString(), anyString())).thenReturn(aiJson("Ức gà", "g"));
            when(userRepository.findById(7L)).thenReturn(Optional.of(user));
            when(aiLogRepository.save(any(AiSuggestionLog.class))).thenAnswer(inv -> {
                AiSuggestionLog saved = inv.getArgument(0);
                saved.setId(55L);
                return saved;
            });

            AiSuggestResponse result = service.suggestFromInput(7L, List.of("ức gà", "sả"));

            assertThat(result.getTitle()).isEqualTo("Gà xào sả ớt");
            assertThat(result.getLogId()).isEqualTo(55L);
            assertThat(result.isCanSave()).isTrue();
            assertThat(result.getIngredients()).hasSize(1);
            assertThat(result.getSteps()).hasSize(1);
        }

        @Test
        void wrapsUnparsableJsonAsAiServiceException() {
            when(geminiConfig.getDailyLimit()).thenReturn(10);
            when(aiLogRepository.countByUserIdAndCreatedAtAfter(eq(7L), any(LocalDateTime.class)))
                    .thenReturn(0L);
            when(geminiClient.generate(anyString(), anyString())).thenReturn("xin lỗi, tôi không thể");

            // JSON hỏng là lỗi phía AI -> 503, không phải 400 của user
            assertThatThrownBy(() -> service.suggestFromInput(7L, List.of("gà")))
                    .isInstanceOf(AiServiceException.class);

            verify(aiLogRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("saveAiRecipe — khớp nguyên liệu")
    class ResolveIngredient {

        private AiSuggestionLog logWith(String ingredientName, String unit) {
            return AiSuggestionLog.builder()
                    .id(55L).user(user).type(AiSuggestionType.FEASIBLE_FINDER)
                    .inputIngredients("gà")
                    .outputResponse(aiJson(ingredientName, unit))
                    .build();
        }

        /** Bắt RecipeRequest mà AiServiceImpl dựng ra để kiểm tra ingredientId/unit. */
        private RecipeRequest captureRecipeRequest(AiSuggestionLog logEntry) {
            when(aiLogRepository.findById(55L)).thenReturn(Optional.of(logEntry));
            when(recipeService.createRecipe(any(RecipeRequest.class), eq(7L)))
                    .thenReturn(RecipeResponse.builder().id(900L).build());

            service.saveAiRecipe(7L, 55L);

            ArgumentCaptor<RecipeRequest> captor = ArgumentCaptor.forClass(RecipeRequest.class);
            verify(recipeService).createRecipe(captor.capture(), eq(7L));
            return captor.getValue();
        }

        @Test
        @DisplayName("khớp chính xác được ưu tiên trước mọi mức khác")
        void prefersExactMatch() {
            when(ingredientRepository.findFirstByNameIgnoreCase("Ức gà tây"))
                    .thenReturn(Optional.of(ingredient(50L, "Ức gà tây", "g")));

            RecipeRequest request = captureRecipeRequest(logWith("Ức gà tây", "g"));

            assertThat(request.getIngredients().get(0).getIngredientId()).isEqualTo(50L);
            // Đã khớp chính xác thì không được quét findAll hay contains nữa
            verify(ingredientRepository, never()).findAll();
            verify(ingredientRepository, never()).findByNameContainingIgnoreCase(anyString());
        }

        @Test
        @DisplayName("AI nói mơ hồ 'Gà' khớp nhiều dòng -> KHÔNG đoán, tạo mới chờ kiểm duyệt")
        void ambiguousNameCreatesPendingIngredientInsteadOfGuessing() {
            when(ingredientRepository.findFirstByNameIgnoreCase("Gà")).thenReturn(Optional.empty());
            // findAll không có tên nào nằm trong chữ "Gà" nên mức 2 trượt
            when(ingredientRepository.findAll()).thenReturn(List.of(
                    ingredient(60L, "Gan gà", "g"),
                    ingredient(61L, "Ức gà có da", "g")
            ));
            // Mức 3 trả nhiều dòng: dữ liệu thật cho thấy tên ngắn nhất chứa "gà"
            // lại là nội tạng (Mề gà/Gan gà), nên đoán là sai
            when(ingredientRepository.findByNameContainingIgnoreCase("Gà")).thenReturn(List.of(
                    ingredient(60L, "Gan gà", "g"),
                    ingredient(61L, "Ức gà có da", "g")
            ));
            when(ingredientRepository.save(any(Ingredient.class))).thenAnswer(inv -> {
                Ingredient saved = inv.getArgument(0);
                saved.setId(291L);
                return saved;
            });

            RecipeRequest request = captureRecipeRequest(logWith("Gà", "g"));

            assertThat(request.getIngredients().get(0).getIngredientId()).isEqualTo(291L);
            verify(ingredientRepository).save(any(Ingredient.class));
        }

        @Test
        @DisplayName("khớp duy nhất theo chiều xuôi thì nhận: 'Cá hồi' -> 'Cá hồi Đại Tây Dương'")
        void uniqueContainsMatchIsAccepted() {
            when(ingredientRepository.findFirstByNameIgnoreCase("Cá hồi")).thenReturn(Optional.empty());
            when(ingredientRepository.findAll()).thenReturn(List.of(ingredient(70L, "Thịt bò xay", "g")));
            when(ingredientRepository.findByNameContainingIgnoreCase("Cá hồi"))
                    .thenReturn(List.of(ingredient(139L, "Cá hồi Đại Tây Dương", "g")));

            RecipeRequest request = captureRecipeRequest(logWith("Cá hồi", "g"));

            assertThat(request.getIngredients().get(0).getIngredientId()).isEqualTo(139L);
            verify(ingredientRepository, never()).save(any(Ingredient.class));
        }

        @Test
        @DisplayName("AI nói dài 'Thịt ức gà tươi' -> lấy tên DB DÀI NHẤT nằm trong đó")
        void reverseContainsPicksLongestName() {
            when(ingredientRepository.findFirstByNameIgnoreCase("Thịt ức gà tươi"))
                    .thenReturn(Optional.empty());
            when(ingredientRepository.findAll()).thenReturn(List.of(
                    ingredient(70L, "Gà", "g"),
                    ingredient(71L, "Ức gà", "g"),
                    ingredient(72L, "Cá hồi", "g")
            ));

            RecipeRequest request = captureRecipeRequest(logWith("Thịt ức gà tươi", "g"));

            assertThat(request.getIngredients().get(0).getIngredientId()).isEqualTo(71L);
            // Khớp được ở mức 2 thì không cần chạy mức 3
            verify(ingredientRepository, never()).findByNameContainingIgnoreCase(anyString());
        }

        @Test
        @DisplayName("dùng baseUnit của DB, bỏ đơn vị AI trả về")
        void overridesAiUnitWithDatabaseBaseUnit() {
            when(ingredientRepository.findFirstByNameIgnoreCase("Trứng gà"))
                    .thenReturn(Optional.of(ingredient(80L, "Trứng gà", "g")));

            // AI trả "quả" — đơn vị này không có đường quy đổi trong unit_conversions
            RecipeRequest request = captureRecipeRequest(logWith("Trứng gà", "quả"));

            assertThat(request.getIngredients().get(0).getUnit()).isEqualTo("g");
        }

        @Test
        @DisplayName("không tìm thấy -> tạo mới với dinh dưỡng 0 và baseUnit 'g'")
        void createsPendingReviewIngredientWhenNoMatch() {
            when(ingredientRepository.findFirstByNameIgnoreCase("Hoa atiso đỏ"))
                    .thenReturn(Optional.empty());
            when(ingredientRepository.findAll()).thenReturn(List.of(ingredient(90L, "Cá hồi", "g")));
            when(ingredientRepository.findByNameContainingIgnoreCase("Hoa atiso đỏ"))
                    .thenReturn(List.of());
            when(ingredientRepository.save(any(Ingredient.class))).thenAnswer(inv -> {
                Ingredient saved = inv.getArgument(0);
                saved.setId(291L);
                return saved;
            });

            RecipeRequest request = captureRecipeRequest(logWith("Hoa atiso đỏ", "bó"));

            ArgumentCaptor<Ingredient> captor = ArgumentCaptor.forClass(Ingredient.class);
            verify(ingredientRepository).save(captor.capture());
            Ingredient created = captor.getValue();

            // calories = 0 là cờ để admin lọc ra nguyên liệu cần kiểm duyệt
            assertThat(created.getCaloriesPer100g()).isEqualByComparingTo("0");
            assertThat(created.getProtein()).isEqualByComparingTo("0");
            // 'g' chứ không phải "bó" mà AI trả về
            assertThat(created.getBaseUnit()).isEqualTo("g");
            assertThat(request.getIngredients().get(0).getIngredientId()).isEqualTo(291L);
            assertThat(request.getIngredients().get(0).getUnit()).isEqualTo("g");
        }
    }

    @Nested
    @DisplayName("getHistory")
    class History {

        @Test
        void mapsTitleAndSavedStateFromLogs() {
            AiSuggestionLog unsaved = AiSuggestionLog.builder()
                    .id(1L).user(user).type(AiSuggestionType.ZERO_WASTE)
                    .inputIngredients("Ức gà — 200 g")
                    .outputResponse(aiJson("Ức gà", "g"))
                    .createdAt(LocalDateTime.of(2026, 8, 27, 9, 0))
                    .build();
            AiSuggestionLog saved = AiSuggestionLog.builder()
                    .id(2L).user(user).type(AiSuggestionType.FEASIBLE_FINDER)
                    .inputIngredients("cá hồi")
                    .outputResponse(aiJson("Cá hồi", "g"))
                    .savedRecipe(Recipe.builder().id(900L).build())
                    .createdAt(LocalDateTime.of(2026, 8, 27, 10, 0))
                    .build();
            when(aiLogRepository.findByUserIdOrderByCreatedAtDesc(7L)).thenReturn(List.of(unsaved, saved));

            List<AiHistoryResponse> history = service.getHistory(7L);

            assertThat(history).hasSize(2);
            assertThat(history.get(0).getTitle()).isEqualTo("Gà xào sả ớt");
            assertThat(history.get(0).getSavedRecipeId()).isNull();
            assertThat(history.get(0).isCanSave()).isTrue();
            assertThat(history.get(1).getSavedRecipeId()).isEqualTo(900L);
            assertThat(history.get(1).isCanSave()).isFalse();
        }

        @Test
        void brokenJsonInOneLogDoesNotFailWholeList() {
            AiSuggestionLog broken = AiSuggestionLog.builder()
                    .id(3L).user(user).type(AiSuggestionType.ZERO_WASTE)
                    .inputIngredients("gà")
                    .outputResponse("không phải JSON")
                    .createdAt(LocalDateTime.of(2026, 8, 27, 11, 0))
                    .build();
            when(aiLogRepository.findByUserIdOrderByCreatedAtDesc(7L)).thenReturn(List.of(broken));

            // Một log cũ hỏng không được làm cả trang lịch sử văng lỗi
            List<AiHistoryResponse> history = service.getHistory(7L);

            assertThat(history).hasSize(1);
            assertThat(history.get(0).getTitle()).isEqualTo("Gợi ý không đọc được");
        }
    }
}

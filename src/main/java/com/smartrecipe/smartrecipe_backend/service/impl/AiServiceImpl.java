package com.smartrecipe.smartrecipe_backend.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartrecipe.smartrecipe_backend.config.GeminiClient;
import com.smartrecipe.smartrecipe_backend.config.GeminiConfig;
import com.smartrecipe.smartrecipe_backend.dto.response.AiHistoryResponse;
import com.smartrecipe.smartrecipe_backend.dto.response.AiRemainingResponse;
import com.smartrecipe.smartrecipe_backend.dto.response.AiSuggestResponse;
import com.smartrecipe.smartrecipe_backend.dto.response.PantryResponse;
import com.smartrecipe.smartrecipe_backend.entity.AiSuggestionLog;
import com.smartrecipe.smartrecipe_backend.entity.User;
import com.smartrecipe.smartrecipe_backend.enums.AiSuggestionType;
import com.smartrecipe.smartrecipe_backend.dto.request.RecipeIngredientRequest;
import com.smartrecipe.smartrecipe_backend.dto.request.RecipeRequest;
import com.smartrecipe.smartrecipe_backend.dto.request.RecipeStepRequest;
import com.smartrecipe.smartrecipe_backend.dto.response.RecipeResponse;
import com.smartrecipe.smartrecipe_backend.entity.Ingredient;
import com.smartrecipe.smartrecipe_backend.enums.Difficulty;
import com.smartrecipe.smartrecipe_backend.enums.RecipeStatus;
import com.smartrecipe.smartrecipe_backend.exception.AiServiceException;
import com.smartrecipe.smartrecipe_backend.exception.BadRequestException;
import com.smartrecipe.smartrecipe_backend.exception.RateLimitExceededException;
import com.smartrecipe.smartrecipe_backend.repository.AiSuggestionLogRepository;
import com.smartrecipe.smartrecipe_backend.repository.IngredientRepository;
import com.smartrecipe.smartrecipe_backend.repository.UserRepository;
import com.smartrecipe.smartrecipe_backend.service.AiService;
import com.smartrecipe.smartrecipe_backend.service.PantryService;
import com.smartrecipe.smartrecipe_backend.service.RecipeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AiServiceImpl implements AiService {

    private static final Logger log = LoggerFactory.getLogger(AiServiceImpl.class);

    /**
     * System Prompt tĩnh — hướng dẫn Gemini trả về JSON khớp cấu trúc RecipeRequest.
     */
    private static final String SYSTEM_PROMPT = """
            Bạn là một đầu bếp chuyên nghiệp người Việt Nam với 20 năm kinh nghiệm.
            Nhiệm vụ: Dựa vào danh sách nguyên liệu mà người dùng cung cấp, hãy gợi ý MỘT công thức nấu ăn phù hợp nhất.
            
            QUY TẮC BẮT BUỘC:
            1. Ưu tiên sử dụng TỐI ĐA các nguyên liệu được cung cấp, đặc biệt những nguyên liệu sắp hết hạn (nếu có ghi chú).
            2. Có thể thêm gia vị cơ bản (muối, đường, dầu ăn, tỏi, hành...) nếu cần, nhưng KHÔNG thêm nguyên liệu chính ngoài danh sách.
            3. Công thức phải thực tế, dễ nấu tại nhà.
            4. Viết bằng tiếng Việt.
            5. Trả về ĐÚNG định dạng JSON sau, KHÔNG thêm bất kỳ text nào ngoài JSON:
            
            {
              "title": "Tên món ăn",
              "description": "Mô tả ngắn về món ăn (2-3 câu)",
              "baseServings": 2,
              "prepTime": 10,
              "cookTime": 15,
              "difficulty": "EASY",
              "ingredients": [
                { "ingredientName": "Thịt bò", "amount": 200, "unit": "g" },
                { "ingredientName": "Hành tây", "amount": 1, "unit": "củ" }
              ],
              "steps": [
                { "stepNumber": 1, "instruction": "Bước thực hiện chi tiết..." },
                { "stepNumber": 2, "instruction": "Bước thực hiện chi tiết..." }
              ]
            }
            
            Lưu ý về giá trị "difficulty": chỉ được dùng 1 trong 3 giá trị: "EASY", "MEDIUM", "HARD".
            Lưu ý về "amount": phải là số (integer hoặc decimal), KHÔNG phải chuỗi.
            """;

    private final GeminiClient geminiClient;
    private final GeminiConfig geminiConfig;
    private final PantryService pantryService;
    private final AiSuggestionLogRepository aiLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final IngredientRepository ingredientRepository;
    private final RecipeService recipeService;

    public AiServiceImpl(GeminiClient geminiClient,
                         GeminiConfig geminiConfig,
                         PantryService pantryService,
                         AiSuggestionLogRepository aiLogRepository,
                         UserRepository userRepository,
                         ObjectMapper objectMapper,
                         IngredientRepository ingredientRepository,
                         RecipeService recipeService) {
        this.geminiClient = geminiClient;
        this.geminiConfig = geminiConfig;
        this.pantryService = pantryService;
        this.aiLogRepository = aiLogRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.ingredientRepository = ingredientRepository;
        this.recipeService = recipeService;
    }

    // ===================== PUBLIC METHODS =====================

    @Override
    @Transactional
    public AiSuggestResponse suggestFromPantry(Long userId) {
        // 1. Rate limit check
        checkRateLimit(userId);

        // 2. Lấy nguyên liệu từ pantry
        Map<String, List<PantryResponse>> pantryMap = pantryService.getMyPantry(userId, "ALL");

        List<PantryResponse> allItems = pantryMap.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());

        if (allItems.isEmpty()) {
            throw new BadRequestException("Tủ nguyên liệu của bạn đang trống. Hãy thêm nguyên liệu trước khi gợi ý.");
        }

        // 3. Sắp xếp: sắp hết hạn lên trước
        allItems.sort(Comparator.comparing(
                item -> item.getDaysUntilExpiry() == null ? Long.MAX_VALUE : item.getDaysUntilExpiry()
        ));

        // 4. Tạo danh sách nguyên liệu cho prompt
        List<String> ingredientTexts = allItems.stream()
                .map(item -> {
                    String name = item.getIngredient().getName();
                    String qty = item.getQuantityAvailable().stripTrailingZeros().toPlainString();
                    String unit = item.getIngredient().getBaseUnit();
                    Long daysLeft = item.getDaysUntilExpiry();
                    String urgency = (daysLeft != null && daysLeft <= 3) ? " ⚠️ SẮP HẾT HẠN" : "";
                    return name + " — " + qty + " " + unit + urgency;
                })
                .toList();

        String inputText = String.join("\n", ingredientTexts);

        // 5. Gọi AI
        String userPrompt = "Dưới đây là danh sách nguyên liệu trong tủ lạnh của tôi:\n\n" + inputText +
                "\n\nHãy gợi ý cho tôi 1 món ăn ngon, ưu tiên dùng nguyên liệu sắp hết hạn trước.";

        String aiResponseJson = geminiClient.generate(SYSTEM_PROMPT, userPrompt);

        // 6. Parse & lưu log
        AiSuggestResponse response = parseAiResponse(aiResponseJson);
        AiSuggestionLog savedLog = saveLog(userId, AiSuggestionType.ZERO_WASTE, inputText, aiResponseJson);
        response.setLogId(savedLog.getId());
        response.setCanSave(true);

        return response;
    }

    @Override
    @Transactional
    public AiSuggestResponse suggestFromInput(Long userId, List<String> ingredients) {
        // 1. Rate limit check
        checkRateLimit(userId);

        // 2. Validate
        if (ingredients == null || ingredients.isEmpty()) {
            throw new BadRequestException("Vui lòng nhập ít nhất 1 nguyên liệu.");
        }

        // 3. Tạo prompt
        String inputText = String.join(", ", ingredients);
        String userPrompt = "Tôi có các nguyên liệu sau: " + inputText +
                "\n\nHãy gợi ý cho tôi 1 món ăn ngon nhất có thể nấu từ các nguyên liệu trên.";

        // 4. Gọi AI
        String aiResponseJson = geminiClient.generate(SYSTEM_PROMPT, userPrompt);

        // 5. Parse & lưu log
        AiSuggestResponse response = parseAiResponse(aiResponseJson);
        AiSuggestionLog savedLog = saveLog(userId, AiSuggestionType.FEASIBLE_FINDER, inputText, aiResponseJson);
        response.setLogId(savedLog.getId());
        response.setCanSave(true);

        return response;
    }

    @Override
    @Transactional
    public RecipeResponse saveAiRecipe(Long userId, Long logId) {
        // 1. Lấy log từ DB
        AiSuggestionLog logEntry = aiLogRepository.findById(logId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy dữ liệu gợi ý AI (Log ID: " + logId + ")"));

        if (!logEntry.getUser().getId().equals(userId)) {
            throw new BadRequestException("Bạn không có quyền lưu công thức của người khác");
        }

        if (logEntry.getSavedRecipe() != null) {
            throw new BadRequestException("Gợi ý này đã được lưu thành công thức rồi");
        }

        // 2. Parse lại JSON response từ log
        AiSuggestResponse aiResponse = parseAiResponse(logEntry.getOutputResponse());

        // 3. Match Ingredients
        List<RecipeIngredientRequest> ingredientRequests = new ArrayList<>();
        for (AiSuggestResponse.IngredientItem item : aiResponse.getIngredients()) {
            RecipeIngredientRequest req = RecipeIngredientRequest.builder()
                    .ingredientName(item.getIngredientName())
                    .amount(item.getAmount())
                    .unit(item.getUnit())
                    .build();

            // Tìm nguyên liệu tương ứng trong DB theo 3 mức ưu tiên (xem resolveIngredient)
            Ingredient matched = resolveIngredient(item.getIngredientName());
            if (matched != null) {
                req.setIngredientId(matched.getId());
                // Dùng baseUnit của nguyên liệu trong DB, không tin đơn vị AI trả về.
                // 290 dòng seed đều là 'g'/'ml'; nếu giữ unit của AI (vd "củ", "quả")
                // thì UnitNormalizationService.toBaseUnit() sẽ ném BadRequestException
                // khi trừ kho, vì unit_conversions không có đường dẫn tới các đơn vị đó.
                req.setUnit(matched.getBaseUnit());
            } else {
                // Chưa có trong DB -> tạo mới với dinh dưỡng = 0 làm cờ chờ kiểm duyệt.
                // baseUnit luôn 'g': đơn vị AI trả về ("củ", "quả", "muỗng") không nằm
                // trong unit_conversions nên sẽ làm chức năng trừ kho vỡ về sau.
                Ingredient newIngredient = Ingredient.builder()
                        .name(item.getIngredientName().trim())
                        .baseUnit("g")
                        .caloriesPer100g(BigDecimal.ZERO)
                        .protein(BigDecimal.ZERO)
                        .fat(BigDecimal.ZERO)
                        .carbs(BigDecimal.ZERO)
                        .build();
                Ingredient savedIngredient = ingredientRepository.save(newIngredient);
                log.warn("AI tạo nguyên liệu mới chưa có dinh dưỡng: id={} name='{}'",
                        savedIngredient.getId(), savedIngredient.getName());
                req.setIngredientId(savedIngredient.getId());
                req.setUnit("g");
            }
            ingredientRequests.add(req);
        }

        // 4. Map steps
        List<RecipeStepRequest> stepRequests = aiResponse.getSteps().stream()
                .map(step -> RecipeStepRequest.builder()
                        .stepNumber(step.getStepNumber())
                        .instruction(step.getInstruction())
                        .build())
                .collect(Collectors.toList());

        // 5. Tạo RecipeRequest
        Difficulty diff;
        try {
            diff = Difficulty.valueOf(aiResponse.getDifficulty().toUpperCase());
        } catch (Exception e) {
            diff = Difficulty.MEDIUM;
        }

        RecipeRequest recipeRequest = RecipeRequest.builder()
                .title(aiResponse.getTitle())
                .description(aiResponse.getDescription())
                .baseServings(aiResponse.getBaseServings() > 0 ? aiResponse.getBaseServings() : 2)
                .prepTime(aiResponse.getPrepTime())
                .cookTime(aiResponse.getCookTime())
                .difficulty(diff)
                .status(RecipeStatus.DRAFT) // AI suggestion được lưu dưới dạng bản nháp
                .ingredients(ingredientRequests)
                .steps(stepRequests)
                .tagIds(new ArrayList<>())
                .build();

        // 6. Gọi RecipeService để tạo công thức
        RecipeResponse savedRecipe = recipeService.createRecipe(recipeRequest, userId);

        // 7. Cập nhật log
        logEntry.setSavedRecipe(com.smartrecipe.smartrecipe_backend.entity.Recipe.builder().id(savedRecipe.getId()).build());
        aiLogRepository.save(logEntry);

        return savedRecipe;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiHistoryResponse> getHistory(Long userId) {
        return aiLogRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::mapToHistoryResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AiRemainingResponse getRemaining(Long userId) {
        LocalDateTime startOfToday = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        long used = aiLogRepository.countByUserIdAndCreatedAtAfter(userId, startOfToday);
        int dailyLimit = geminiConfig.getDailyLimit();

        return AiRemainingResponse.builder()
                .used(used)
                .dailyLimit(dailyLimit)
                // Math.max để không trả số âm nếu dailyLimit bị hạ xuống sau khi
                // user đã dùng nhiều lượt trong ngày.
                .remaining(Math.max(0, dailyLimit - used))
                .build();
    }

    // ===================== PRIVATE HELPERS =====================

    /**
     * Chuyển AiSuggestionLog thành dòng lịch sử tóm tắt.
     *
     * <p>Chỉ đọc trường {@code title} từ JSON thay vì parse toàn bộ qua
     * {@link #parseAiResponse(String)} — hàm đó ném {@link BadRequestException} khi
     * JSON hỏng, và một log cũ lỗi không nên làm cả trang lịch sử trả về 400.
     */
    private AiHistoryResponse mapToHistoryResponse(AiSuggestionLog entry) {
        String title = "Gợi ý không đọc được";
        try {
            title = objectMapper.readTree(entry.getOutputResponse())
                    .path("title")
                    .asText("Món ăn gợi ý");
        } catch (Exception e) {
            log.warn("Log AI id={} có outputResponse không parse được: {}", entry.getId(), e.getMessage());
        }

        Long savedRecipeId = entry.getSavedRecipe() != null ? entry.getSavedRecipe().getId() : null;

        return AiHistoryResponse.builder()
                .logId(entry.getId())
                .type(entry.getType() != null ? entry.getType().name() : null)
                .title(title)
                .inputIngredients(entry.getInputIngredients())
                .savedRecipeId(savedRecipeId)
                .canSave(savedRecipeId == null)
                .createdAt(entry.getCreatedAt())
                .build();
    }

    /**
     * Tìm nguyên liệu trong DB khớp với tên AI trả về.
     *
     * <p>Trước đây hàm này chỉ gọi {@code findByNameContainingIgnoreCase} rồi lấy
     * {@code matches.get(0)}. Với 290 nguyên liệu thì cách đó sai thường xuyên:
     * AI trả về "Gà" khớp cả "Gan gà", "Mề gà", "Ức gà có da"... rồi lấy bừa dòng
     * đầu theo thứ tự DB.
     *
     * <p>Thứ tự tìm:
     * <ol>
     *   <li><b>Khớp chính xác</b> (không phân biệt hoa/thường): "Trứng gà" → "Trứng gà".</li>
     *   <li><b>Tên AI chứa tên DB</b>, chọn tên DB DÀI NHẤT: "Thịt ức gà tươi" →
     *       "Ức gà tây"? không — chọn tên dài nhất thực sự nằm trong câu, ở đây là
     *       "Ức gà" nếu có. Chiều này an toàn vì tên DB càng dài càng khớp nhiều
     *       chữ của AI.</li>
     *   <li><b>Tên DB chứa tên AI</b> và CHỈ CÓ ĐÚNG MỘT kết quả: "Cá hồi" →
     *       "Cá hồi Đại Tây Dương".</li>
     * </ol>
     *
     * <p><b>Cố tình không đoán khi mức 3 có nhiều kết quả.</b> Ban đầu tôi định
     * chọn tên ngắn nhất (giả định tên ngắn = tên chung nhất), nhưng kiểm tra dữ
     * liệu thật thì sai: dataset USDA không có "Thịt gà" trơn, tên ngắn nhất chứa
     * "gà" là "Mề gà"/"Gan gà"/"Tim gà"; chứa "bò" là "Cật bò"/"Gan bò". Chọn ngắn
     * nhất sẽ map "Gà" → "Mề gà". Nội tạng có tên ngắn hơn thịt nạc, nên heuristic
     * đó phản tác dụng.
     *
     * <p>Khi mơ hồ, trả {@code null} để caller tạo nguyên liệu mới với dinh dưỡng
     * = 0. Một dòng chờ kiểm duyệt vẫn tốt hơn một công thức gắn sai nguyên liệu
     * mà không ai biết.
     *
     * @return nguyên liệu khớp, hoặc {@code null} nếu không khớp chắc chắn
     */
    private Ingredient resolveIngredient(String aiName) {
        if (aiName == null || aiName.isBlank()) {
            return null;
        }
        String name = aiName.trim();

        // Mức 1: khớp chính xác
        var exact = ingredientRepository.findFirstByNameIgnoreCase(name);
        if (exact.isPresent()) {
            return exact.get();
        }

        // Mức 2: tên AI chứa tên trong DB -> lấy tên dài nhất (cụ thể nhất).
        // Duyệt toàn bộ bảng; 290 dòng nên chi phí không đáng kể.
        String lowerAiName = name.toLowerCase();
        Ingredient reverseMatch = null;
        int longestMatch = 0;
        for (Ingredient candidate : ingredientRepository.findAll()) {
            String candidateName = candidate.getName();
            if (candidateName == null || candidateName.isBlank()) {
                continue;
            }
            if (lowerAiName.contains(candidateName.toLowerCase()) && candidateName.length() > longestMatch) {
                longestMatch = candidateName.length();
                reverseMatch = candidate;
            }
        }
        if (reverseMatch != null) {
            log.info("Khớp nguyên liệu theo chiều ngược: AI='{}' -> DB='{}'", name, reverseMatch.getName());
            return reverseMatch;
        }

        // Mức 3: tên DB chứa tên AI, chỉ nhận khi duy nhất
        List<Ingredient> contains = ingredientRepository.findByNameContainingIgnoreCase(name);
        if (contains.size() == 1) {
            Ingredient only = contains.get(0);
            log.info("Khớp nguyên liệu duy nhất theo chiều xuôi: AI='{}' -> DB='{}'", name, only.getName());
            return only;
        }
        if (contains.size() > 1) {
            log.warn("Tên nguyên liệu '{}' khớp {} dòng trong DB, không đoán -> tạo mới chờ kiểm duyệt",
                    name, contains.size());
        }
        return null;
    }

    /**
     * Kiểm tra rate limit: tối đa N lần/ngày/user (dựa vào bảng ai_suggestion_logs).
     */
    private void checkRateLimit(Long userId) {
        LocalDateTime startOfToday = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        long todayCount = aiLogRepository.countByUserIdAndCreatedAtAfter(userId, startOfToday);

        int dailyLimit = geminiConfig.getDailyLimit();
        if (todayCount >= dailyLimit) {
            throw new RateLimitExceededException(
                    "Bạn đã sử dụng hết " + dailyLimit + " lượt gợi ý AI trong hôm nay. Vui lòng thử lại vào ngày mai."
            );
        }
    }

    /**
     * Parse JSON response từ Gemini thành AiSuggestResponse.
     */
    private AiSuggestResponse parseAiResponse(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);

            // Parse ingredients
            List<AiSuggestResponse.IngredientItem> ingredients = new ArrayList<>();
            JsonNode ingNode = root.path("ingredients");
            if (ingNode.isArray()) {
                for (JsonNode item : ingNode) {
                    ingredients.add(AiSuggestResponse.IngredientItem.builder()
                            .ingredientName(item.path("ingredientName").asText())
                            .amount(new BigDecimal(item.path("amount").asText("0")))
                            .unit(item.path("unit").asText(""))
                            .build());
                }
            }

            // Parse steps
            List<AiSuggestResponse.StepItem> steps = new ArrayList<>();
            JsonNode stepsNode = root.path("steps");
            if (stepsNode.isArray()) {
                for (JsonNode step : stepsNode) {
                    steps.add(AiSuggestResponse.StepItem.builder()
                            .stepNumber(step.path("stepNumber").asInt())
                            .instruction(step.path("instruction").asText())
                            .build());
                }
            }

            return AiSuggestResponse.builder()
                    .title(root.path("title").asText("Món ăn gợi ý"))
                    .description(root.path("description").asText(""))
                    .baseServings(root.path("baseServings").asInt(2))
                    .prepTime(root.path("prepTime").asInt(10))
                    .cookTime(root.path("cookTime").asInt(15))
                    .difficulty(root.path("difficulty").asText("EASY"))
                    .ingredients(ingredients)
                    .steps(steps)
                    .build();

        } catch (Exception e) {
            // JSON hỏng là lỗi phía AI, không phải do user nhập sai -> 503 chứ không 400
            log.error("Failed to parse AI response JSON: {}", e.getMessage());
            throw new AiServiceException("Không thể phân tích kết quả từ AI. Vui lòng thử lại.", e);
        }
    }

    /**
     * Lưu log gợi ý AI vào database.
     */
    private AiSuggestionLog saveLog(Long userId, AiSuggestionType type, String input, String output) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User không tồn tại"));

        AiSuggestionLog logEntry = AiSuggestionLog.builder()
                .user(user)
                .type(type)
                .inputIngredients(input)
                .outputResponse(output)
                .build();

        return aiLogRepository.save(logEntry);
    }
}

    /**
     * Chuyển danh sách chuỗi thành JSON array string để lưu vào cột JSON của MySQL.
     * Ví dụ: ["Thịt gà — 500 g ⚠️", "Cà rốt — 200 g"] → "[\"Thịt gà — 500 g ⚠️\",\"Cà rốt — 200 g\"]"
     */
    private String toJsonArray(List<String> items) {
        try {
            return objectMapper.writeValueAsString(items);
        } catch (Exception e) {
            // Fallback: JSON array với 1 phần tử là toàn bộ text
            return "[\"" + String.join(", ", items).replace("\"", "'") + "\"]";
        }
    }

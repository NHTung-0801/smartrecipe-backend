package com.smartrecipe.smartrecipe_backend.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartrecipe.smartrecipe_backend.exception.AiServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/**
 * Client gọi trực tiếp Google Gemini API.
 * Gửi prompt và nhận phản hồi text từ model.
 */
@Component
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    private final RestClient geminiRestClient;
    private final GeminiConfig geminiConfig;
    private final ObjectMapper objectMapper;

    public GeminiClient(RestClient geminiRestClient, GeminiConfig geminiConfig, ObjectMapper objectMapper) {
        this.geminiRestClient = geminiRestClient;
        this.geminiConfig = geminiConfig;
        this.objectMapper = objectMapper;
    }

    /**
     * Gọi Gemini API với system prompt + user prompt.
     * Trả về text content từ response.
     *
     * @param systemPrompt Prompt hệ thống (hướng dẫn AI cách trả lời)
     * @param userPrompt   Prompt người dùng (dữ liệu đầu vào)
     * @return Chuỗi text phản hồi từ Gemini
     */
    public String generate(String systemPrompt, String userPrompt) {
        String model = geminiConfig.getModel();
        String apiKey = geminiConfig.getApiKey();

        // Cấu trúc request body theo Gemini API spec
        Map<String, Object> requestBody = Map.of(
                "system_instruction", Map.of(
                        "parts", List.of(Map.of("text", systemPrompt))
                ),
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", userPrompt)))
                ),
                "generationConfig", Map.of(
                        "temperature", 0.7,
                        "maxOutputTokens", 4096,
                        "responseMimeType", "application/json"
                )
        );

        String url = "/models/" + model + ":generateContent?key=" + apiKey;

        log.info("Calling Gemini API: model={}", model);

        String responseJson;
        try {
            responseJson = geminiRestClient.post()
                    .uri(url)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException e) {
            // Bao gồm timeout (read/connect), mất mạng, HTTP 4xx/5xx từ Gemini.
            // Không log e.getMessage() kèm url vì url chứa API key.
            log.error("Gemini API call failed: {}", e.getClass().getSimpleName(), e);
            throw new AiServiceException(
                    "Không kết nối được tới dịch vụ AI. Vui lòng thử lại sau ít phút.", e);
        }

        return extractTextFromResponse(responseJson);
    }

    /**
     * Trích xuất text content từ Gemini response JSON.
     * Cấu trúc: { candidates: [{ content: { parts: [{ text: "..." }] } }] }
     */
    private String extractTextFromResponse(String responseJson) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode candidates = root.path("candidates");

            if (candidates.isArray() && !candidates.isEmpty()) {
                JsonNode text = candidates.get(0)
                        .path("content")
                        .path("parts")
                        .get(0)
                        .path("text");

                if (!text.isMissingNode()) {
                    return text.asText();
                }
            }

            log.warn("Gemini response has no valid candidates: {}", responseJson);
            throw new AiServiceException("Dịch vụ AI trả về kết quả không hợp lệ. Vui lòng thử lại.");

        } catch (JsonProcessingException e) {
            log.error("Failed to parse Gemini response: {}", e.getMessage());
            throw new AiServiceException("Không thể phân tích phản hồi từ dịch vụ AI.", e);
        }
    }
}

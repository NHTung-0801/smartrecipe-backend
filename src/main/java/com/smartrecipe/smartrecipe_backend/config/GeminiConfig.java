package com.smartrecipe.smartrecipe_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Cấu hình kết nối tới Google Gemini API.
 * RestClient được khởi tạo với base URL chuẩn của Gemini;
 * API key sẽ được truyền dưới dạng query parameter khi gọi.
 */
@Configuration
public class GeminiConfig {

    /**
     * Timeout khi mở kết nối TCP. Ngắn vì nếu không bắt tay được trong 5s thì
     * gần như chắc là mất mạng, chờ thêm cũng vô nghĩa.
     */
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);

    /**
     * Timeout khi chờ Gemini trả lời. Sinh công thức thường mất 3-10s nên phải
     * để rộng; 30s là mức chặn được request treo vô hạn mà vẫn không cắt oan
     * các lần trả lời chậm.
     *
     * <p>Không có timeout thì thread của Tomcat bị giữ vô thời hạn khi Gemini
     * treo, và người dùng ngồi chờ một spinner không bao giờ kết thúc.
     */
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(30);

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.model:gemini-3-flash-preview}")
    private String model;

    @Value("${gemini.daily-limit:10}")
    private int dailyLimit;

    @Bean
    public RestClient geminiRestClient() {
        // Spring Boot 4 đã bỏ org.springframework.boot.web.client.*, nên cấu hình
        // timeout trực tiếp trên request factory: connect timeout thuộc HttpClient
        // của JDK, read timeout thuộc factory.
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(READ_TIMEOUT);

        return RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .defaultHeader("Content-Type", "application/json")
                .requestFactory(requestFactory)
                .build();
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getModel() {
        return model;
    }

    public int getDailyLimit() {
        return dailyLimit;
    }
}

package com.smartrecipe.smartrecipe_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Cấu hình UTF-8 encoding cho toàn bộ request/response
 * Giải quyết vấn đề mojibake (lỗi phông chữ tiếng Việt) trên Windows
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Bean
    public CharacterEncodingFilter characterEncodingFilter() {
        CharacterEncodingFilter filter = new CharacterEncodingFilter();
        filter.setEncoding("UTF-8");
        filter.setForceEncoding(true);
        filter.setForceRequestEncoding(true);
        filter.setForceResponseEncoding(true);
        return filter;
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        // Tìm và thay thế StringHttpMessageConverter mặc định bằng UTF-8 version
        // Dùng extendMessageConverters (không phải configureMessageConverters) 
        // để không phá vỡ các converter mặc định của Spring Boot (đặc biệt Jackson)
        converters.stream()
            .filter(c -> c instanceof StringHttpMessageConverter)
            .map(c -> (StringHttpMessageConverter) c)
            .forEach(c -> c.setDefaultCharset(StandardCharsets.UTF_8));
    }
}

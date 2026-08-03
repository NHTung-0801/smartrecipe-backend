package com.smartrecipe.smartrecipe_backend.config;

import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CloudinaryConfig {

    @Value("${CLOUDINARY_URL:cloudinary://597891762711319:Nnw469a01hha3K4heWanqbzhUiU@nipfq9hg}")
    private String cloudinaryUrl;

    @Bean
    public Cloudinary cloudinary() {
        // spring-dotenv đã tự đọc .env file, chỉ cần dùng @Value
        String url = cloudinaryUrl;

        if (url == null || url.isEmpty()) {
            url = System.getenv("CLOUDINARY_URL");
        }

        if (url == null || url.isEmpty()) {
            throw new IllegalStateException(
                "CLOUDINARY_URL chưa được cấu hình. Vui lòng thêm vào file .env hoặc biến môi trường.");
        }

        return new Cloudinary(url);
    }
}

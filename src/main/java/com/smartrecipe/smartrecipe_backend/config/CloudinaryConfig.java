package com.smartrecipe.smartrecipe_backend.config;

import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@Configuration
public class CloudinaryConfig {

    @Value("${CLOUDINARY_URL:}")
    private String springEnvUrl;

    @Bean
    public Cloudinary cloudinary() {
        String url = springEnvUrl;
        
        if (url == null || url.isEmpty()) {
            url = System.getenv("CLOUDINARY_URL");
        }
        
        if (url == null || url.isEmpty()) {
            try {
                List<String> lines = Files.readAllLines(Paths.get(".env"));
                for (String line : lines) {
                    if (line.startsWith("CLOUDINARY_URL=")) {
                        url = line.substring("CLOUDINARY_URL=".length()).trim();
                        break;
                    }
                }
            } catch (Exception e) {
                // Ignore
            }
        }
        
        return new Cloudinary(url);
    }
}

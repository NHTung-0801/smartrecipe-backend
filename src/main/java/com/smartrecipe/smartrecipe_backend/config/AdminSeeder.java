package com.smartrecipe.smartrecipe_backend.config;

import com.smartrecipe.smartrecipe_backend.entity.User;
import com.smartrecipe.smartrecipe_backend.enums.Role;
import com.smartrecipe.smartrecipe_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        final String ADMIN_USERNAME = "admin123";
        final String ADMIN_EMAIL = "admin@smartrecipe.local";

        if (!userRepository.existsByUsername(ADMIN_USERNAME)) {
            User admin = User.builder()
                    .username(ADMIN_USERNAME)
                    .email(ADMIN_EMAIL)
                    .passwordHash(passwordEncoder.encode("admin123"))
                    .displayName("Quản trị viên")
                    .role(Role.ADMIN)
                    .build();
            userRepository.save(admin);
            log.info("[AdminSeeder] Đã tạo tài khoản admin mặc định: username={}", ADMIN_USERNAME);
        } else {
            log.debug("[AdminSeeder] Tài khoản admin đã tồn tại, bỏ qua.");
        }
    }
}

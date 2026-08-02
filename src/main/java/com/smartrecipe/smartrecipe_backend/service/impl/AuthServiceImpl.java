package com.smartrecipe.smartrecipe_backend.service.impl;

import com.smartrecipe.smartrecipe_backend.dto.request.LoginRequest;
import com.smartrecipe.smartrecipe_backend.dto.request.RegisterRequest;
import com.smartrecipe.smartrecipe_backend.dto.response.AuthResponse;
import com.smartrecipe.smartrecipe_backend.entity.User;
import com.smartrecipe.smartrecipe_backend.enums.Role;
import com.smartrecipe.smartrecipe_backend.exception.DuplicateResourceException;
import com.smartrecipe.smartrecipe_backend.exception.ResourceNotFoundException;
import com.smartrecipe.smartrecipe_backend.repository.UserRepository;
import com.smartrecipe.smartrecipe_backend.security.JwtProvider;
import com.smartrecipe.smartrecipe_backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;

    @Override
    public void register(RegisterRequest registerRequest) {
        // Kiểm tra xem Username đã tồn tại chưa
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new DuplicateResourceException("Username đã được sử dụng!");
        }

        // Kiểm tra xem Email đã tồn tại chưa
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new DuplicateResourceException("Email đã được sử dụng!");
        }

        // Tạo User mới và băm mật khẩu
        User user = User.builder()
                .username(registerRequest.getUsername())
                .email(registerRequest.getEmail())
                .passwordHash(passwordEncoder.encode(registerRequest.getPassword()))
                .role(Role.USER) // Mặc định tất cả user mới đăng ký đều có quyền USER
                .build();

        userRepository.save(user);
    }

    @Override
    public AuthResponse login(LoginRequest loginRequest) {
        // Ủy quyền cho Spring Security xác thực username và password
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        // Set context cho phiên hiện tại
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Sinh Access Token & Refresh Token
        String jwt = jwtProvider.generateToken(authentication);
        String refreshToken = jwtProvider.generateRefreshToken(loginRequest.getUsername());
        
        // Lấy thông tin user (đã được xác thực nên chắc chắn tồn tại)
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy User"));

        return AuthResponse.builder()
                .accessToken(jwt)
                .refreshToken(refreshToken)
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
}

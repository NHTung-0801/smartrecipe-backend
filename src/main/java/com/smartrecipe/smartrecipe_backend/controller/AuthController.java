package com.smartrecipe.smartrecipe_backend.controller;

import com.smartrecipe.smartrecipe_backend.dto.request.LoginRequest;
import com.smartrecipe.smartrecipe_backend.dto.request.RegisterRequest;
import com.smartrecipe.smartrecipe_backend.dto.response.ApiResponse;
import com.smartrecipe.smartrecipe_backend.dto.response.AuthResponse;
import com.smartrecipe.smartrecipe_backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest registerRequest) {
        authService.register(registerRequest);
        return new ResponseEntity<>(ApiResponse.success(null, "Đăng ký tài khoản thành công!"), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        AuthResponse authResponse = authService.login(loginRequest);
        return ResponseEntity.ok(ApiResponse.success(authResponse, "Đăng nhập thành công!"));
    }
}

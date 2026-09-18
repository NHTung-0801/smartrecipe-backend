package com.smartrecipe.smartrecipe_backend.service;

import com.smartrecipe.smartrecipe_backend.dto.request.ForgotPasswordRequest;
import com.smartrecipe.smartrecipe_backend.dto.request.LoginRequest;
import com.smartrecipe.smartrecipe_backend.dto.request.RefreshTokenRequest;
import com.smartrecipe.smartrecipe_backend.dto.request.RegisterRequest;
import com.smartrecipe.smartrecipe_backend.dto.request.ResetPasswordRequest;
import com.smartrecipe.smartrecipe_backend.dto.response.AuthResponse;

public interface AuthService {
    void register(RegisterRequest registerRequest);
    AuthResponse login(LoginRequest loginRequest);
    AuthResponse refreshToken(RefreshTokenRequest request);
    void logout(String username);
    void forgotPassword(ForgotPasswordRequest request);
    void resetPassword(ResetPasswordRequest request);
}


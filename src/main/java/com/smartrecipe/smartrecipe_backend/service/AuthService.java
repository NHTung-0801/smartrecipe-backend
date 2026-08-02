package com.smartrecipe.smartrecipe_backend.service;

import com.smartrecipe.smartrecipe_backend.dto.request.LoginRequest;
import com.smartrecipe.smartrecipe_backend.dto.request.RegisterRequest;
import com.smartrecipe.smartrecipe_backend.dto.response.AuthResponse;

public interface AuthService {
    void register(RegisterRequest registerRequest);
    AuthResponse login(LoginRequest loginRequest);
}

package com.akash.pooler_backend.service;

import com.akash.pooler_backend.dto.request.*;
import com.akash.pooler_backend.dto.response.AuthResponse;
import com.akash.pooler_backend.dto.response.TokenRefreshResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest req, HttpServletRequest httpReq);
    AuthResponse login(LoginRequest req, HttpServletRequest httpReq);
    TokenRefreshResponse refresh(RefreshTokenRequest req);
    void logout(String accessToken, HttpServletRequest httpReq);
    void logoutAll(String accessToken);
    void forgotPassword(ForgotPasswordRequest req, HttpServletRequest httpReq);
    void resetPassword(ResetPasswordRequest req);
}

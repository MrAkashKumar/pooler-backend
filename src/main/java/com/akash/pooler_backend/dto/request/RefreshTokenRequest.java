package com.akash.pooler_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @author Akash Kumar
 */
@Data
public class RefreshTokenRequest {
    @NotBlank(message="Refresh token is required")
    private String refreshToken;
}

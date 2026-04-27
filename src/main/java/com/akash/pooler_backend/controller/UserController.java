package com.akash.pooler_backend.controller;

import com.akash.pooler_backend.dto.request.ChangePasswordRequest;
import com.akash.pooler_backend.dto.request.UpdateProfileRequest;
import com.akash.pooler_backend.dto.response.ApiResponse;
import com.akash.pooler_backend.dto.response.UserResponse;
import com.akash.pooler_backend.entity.PbUserEntity;
import com.akash.pooler_backend.interceptors.annotation.CurrentUser;
import com.akash.pooler_backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * @author Akash Kumar
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "User Profile", description = "Profile management for authenticated users")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(@CurrentUser PbUserEntity pbUserEntity) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getProfile(pbUserEntity)));
    }

    @PutMapping("/me")
    @Operation(summary = "Update profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @CurrentUser PbUserEntity pbUserEntity,
            @Valid @RequestBody UpdateProfileRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Profile updated", userService.updateProfile(pbUserEntity, req)));
    }

    @PutMapping("/me/change-password")
    @Operation(summary = "Change password (requires current password)")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @CurrentUser PbUserEntity pbUserEntity,
            @Valid @RequestBody ChangePasswordRequest req) {
        userService.changePassword(pbUserEntity, req);
        return ResponseEntity.ok(ApiResponse.message("Password changed. Please login again."));
    }

    @DeleteMapping("/me")
    @Operation(summary = "Delete account permanently")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(@CurrentUser PbUserEntity pbUserEntity) {
        userService.deleteAccount(pbUserEntity);
        return ResponseEntity.ok(ApiResponse.message("Account deleted successfully"));
    }
}

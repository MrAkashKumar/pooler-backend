package com.akash.pooler_backend.controller;

import com.akash.pooler_backend.dto.response.ApiResponse;
import com.akash.pooler_backend.dto.response.UserResponse;
import com.akash.pooler_backend.entity.PbUserEntity;
import com.akash.pooler_backend.enums.UserStatus;
import com.akash.pooler_backend.exception.UserNotFoundException;
import com.akash.pooler_backend.interceptors.annotation.CurrentUser;
import com.akash.pooler_backend.repository.PbUserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/v1/admin")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Admin-only user management")
public class AdminController {

    private final PbUserRepository userRepo;

    @GetMapping("/users")
    @Operation(summary = "List all users (paginated)")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> listUsers(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sort).descending());
        Page<UserResponse> result = userRepo.findAll(pageable).map(UserResponse::from);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable Long id) {
        PbUserEntity user = userRepo.findById(id).orElseThrow(() -> new UserNotFoundException(id.toString()));
        return ResponseEntity.ok(ApiResponse.ok(UserResponse.from(user)));
    }

    @PutMapping("/users/{id}/suspend")
    @Operation(summary = "Suspend a user account")
    public ResponseEntity<ApiResponse<Void>> suspendUser(@PathVariable Long id,
                                                         @CurrentUser PbUserEntity admin) {
        PbUserEntity pbUserEntity = userRepo.findById(id).orElseThrow(() -> new UserNotFoundException(id.toString()));
        pbUserEntity.setStatus(UserStatus.SUSPENDED);
        userRepo.save(pbUserEntity);
        return ResponseEntity.ok(ApiResponse.message("User suspended: " + pbUserEntity.getEmail()));
    }

    @PutMapping("/users/{id}/activate")
    @Operation(summary = "Re-activate a suspended user")
    public ResponseEntity<ApiResponse<Void>> activateUser(@PathVariable Long id) {
        PbUserEntity pbUserEntity = userRepo.findById(id).orElseThrow(() -> new UserNotFoundException(id.toString()));
        pbUserEntity.setStatus(UserStatus.ACTIVE);
        pbUserEntity.resetFailedAttempts();
        userRepo.save(pbUserEntity);
        return ResponseEntity.ok(ApiResponse.message("User activated: " + pbUserEntity.getEmail()));
    }
}

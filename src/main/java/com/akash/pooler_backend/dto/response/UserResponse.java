package com.akash.pooler_backend.dto.response;

import com.akash.pooler_backend.entity.PbUserEntity;
import com.akash.pooler_backend.enums.Role;
import com.akash.pooler_backend.enums.UserStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class UserResponse {

    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private Role role;
    private UserStatus status;
    private Instant createdAt;
    private Instant lastLoginAt;
    private String profilePictureUrl;

}

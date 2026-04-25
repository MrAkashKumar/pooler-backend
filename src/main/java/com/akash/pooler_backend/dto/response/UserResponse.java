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

    private String entityId;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private Role role;
    private UserStatus status;
    private Instant createdAt;
    private Instant lastLoginAt;
    private String profilePictureUrl;

    public static UserResponse from(PbUserEntity pbUserEntity) {
        return UserResponse.builder()
                .entityId(pbUserEntity.getEntityId()).email(pbUserEntity.getEmail())
                .firstName(pbUserEntity.getFirstName()).lastName(pbUserEntity.getLastName())
                .fullName(pbUserEntity.getFullName()).role(pbUserEntity.getRole())
                .status(pbUserEntity.getStatus()).createdAt(pbUserEntity.getCreatedAt())
                .lastLoginAt(pbUserEntity.getLastLoginAt())
                .profilePictureUrl(pbUserEntity.getProfilePictureUrl())
                .build();
    }

}

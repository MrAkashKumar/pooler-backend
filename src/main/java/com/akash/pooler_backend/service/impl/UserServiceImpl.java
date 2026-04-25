package com.akash.pooler_backend.service.impl;

import com.akash.pooler_backend.dto.request.ChangePasswordRequest;
import com.akash.pooler_backend.dto.request.UpdateProfileRequest;
import com.akash.pooler_backend.dto.response.UserResponse;
import com.akash.pooler_backend.entity.PbUserEntity;
import com.akash.pooler_backend.exception.AuthenticationException;
import com.akash.pooler_backend.interceptors.annotation.AuditAction;
import com.akash.pooler_backend.repository.PbUserRepository;
import com.akash.pooler_backend.service.TokenService;
import com.akash.pooler_backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * @author Akash Kumar
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final PbUserRepository pbUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    @Override
    @Transactional(readOnly = true)
    public UserResponse getProfile(PbUserEntity user) {
        return UserResponse.from(user);
    }


    @Override
    @Transactional
    @AuditAction("PROFILE_UPDATE")
    public UserResponse updateProfile(PbUserEntity pbUserEntity, UpdateProfileRequest req) {
        if (req.getFirstName() != null)
            pbUserEntity.setFirstName(req.getFirstName().trim());
        if (req.getLastName() != null)
            pbUserEntity.setLastName(req.getLastName().trim());
        if (req.getProfilePictureUrl() != null)
            pbUserEntity.setProfilePictureUrl(req.getProfilePictureUrl());
        return UserResponse.from(pbUserRepository.save(pbUserEntity));
    }

    @Override
    public void changePassword(PbUserEntity pbUserEntity, ChangePasswordRequest req) {
        if (!passwordEncoder.matches(req.getCurrentPassword(), pbUserEntity.getPasswordHash())) {
            throw new AuthenticationException("Current password is incorrect");
        }
        if (!req.getNewPassword().equals(req.getConfirmPassword())) {
            throw new AuthenticationException("New passwords do not match");
        }
        if (passwordEncoder.matches(req.getNewPassword(), pbUserEntity.getPasswordHash())) {
            throw new AuthenticationException("New password must differ from current");
        }
        pbUserEntity.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        pbUserRepository.save(pbUserEntity);
        tokenService.revokeAllUserTokens(pbUserEntity); // Security: force re-login everywhere
        log.info("Password changed for userId={}", pbUserEntity.getEntityId());

    }

    @Override
    @Transactional
    @AuditAction("ACCOUNT_DELETE")
    public void deleteAccount(PbUserEntity pbUserEntity) {
        tokenService.revokeAllUserTokens(pbUserEntity);
        pbUserRepository.delete(pbUserEntity);
        log.info("Account deleted for userId={}", pbUserEntity.getEntityId());

    }

    @Override
    public PbUserEntity getUserEntity(String entityId) {
        Optional<PbUserEntity> pbUserEntityOptional = pbUserRepository.findByEntityId(entityId);
        return pbUserEntityOptional.orElse(null);
    }


}

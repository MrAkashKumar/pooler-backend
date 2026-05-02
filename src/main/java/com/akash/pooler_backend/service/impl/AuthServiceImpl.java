package com.akash.pooler_backend.service.impl;

import com.akash.pooler_backend.config.AppProperties;
import com.akash.pooler_backend.dto.request.*;
import com.akash.pooler_backend.dto.response.AuthResponse;
import com.akash.pooler_backend.dto.response.TokenRefreshResponse;
import com.akash.pooler_backend.dto.response.UserResponse;
import com.akash.pooler_backend.entity.*;
import com.akash.pooler_backend.enums.TokenStatus;
import com.akash.pooler_backend.enums.UserStatus;
import com.akash.pooler_backend.exception.*;
import com.akash.pooler_backend.interceptors.annotation.AuditAction;
import com.akash.pooler_backend.repository.PbEntitySequenceRepository;
import com.akash.pooler_backend.repository.PbPasswordResetTokenRepository;
import com.akash.pooler_backend.repository.PbRefreshTokenRepository;
import com.akash.pooler_backend.repository.PbUserRepository;
import com.akash.pooler_backend.security.JwtUtil;
import com.akash.pooler_backend.service.AuthService;
import com.akash.pooler_backend.service.MailService;
import com.akash.pooler_backend.service.TokenService;
import com.akash.pooler_backend.service.UserService;
import com.akash.pooler_backend.utils.RequestUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * @author Akash Kumar
 *
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authManager;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TokenService tokenService;
    private final MailService mailService;
    private final AppProperties props;
    private final PbUserRepository userRepo;
    private final PbRefreshTokenRepository refreshTokenRepo;
    private final PbPasswordResetTokenRepository resetTokenRepo;
    private final PbEntitySequenceRepository pbEntitySequenceRepository;
    private final UserService userService;


    // ── Register ──────────────────────────────────────────────────────

    @Override
    @Transactional
    @AuditAction("USER_REGISTER")
    public AuthResponse register(RegisterRequest req, HttpServletRequest httpReq) {
        if (userRepo.existsByEmail(req.getEmail())) {
            throw new UserAlreadyExistsException(req.getEmail());
        }

        //Get entity id
        PbEntityIdSequence pbEntityIdSequence  = pbEntitySequenceRepository.save(new PbEntityIdSequence());

        //Save user id
        PbUserEntity pbUserEntity = PbUserEntity.builder()
                .email(req.getEmail().toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .entityId(Long.toString(pbEntityIdSequence.getId()))
                .firstName(req.getFirstName().trim())
                .lastName(req.getLastName().trim())
                .status(UserStatus.ACTIVE)
                .build();

        pbUserEntity = userRepo.save(pbUserEntity);
        log.info("New pbUserEntity registered: {}", pbUserEntity.getEmail());



        mailService.sendWelcomeMail(pbUserEntity);
        return buildAuthResponse(pbUserEntity, httpReq);
    }

    // ── Login ─────────────────────────────────────────────────────────

    @Override
    @Transactional
    @AuditAction("USER_LOGIN")
    public AuthResponse login(LoginRequest req, HttpServletRequest httpReq) {
        PbUserEntity pbUserEntity = userRepo.findByEmail(req.getEmail().toLowerCase())
                .orElseThrow(() -> new AuthenticationException("Invalid credentials"));

        checkAccountStatus(pbUserEntity);

        try {
            authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));
        } catch (BadCredentialsException e) {
            handleFailedLogin(pbUserEntity);
            throw new AuthenticationException("Invalid credentials");
        }

        pbUserEntity.resetFailedAttempts();
        userRepo.updateLoginSuccess(pbUserEntity.getEntityId(), Instant.now());

        log.info("User logged in: {} from platform={}", pbUserEntity.getEmail(), req.getPlatform());
        return buildAuthResponse(pbUserEntity, httpReq);
    }

    // ── Refresh Token ─────────────────────────────────────────────────

    @Override
    @Transactional
    public TokenRefreshResponse refresh(RefreshTokenRequest req) {
        PbRefreshTokenEntity pbRefreshTokenEntity = tokenService.validateRefreshToken(req.getRefreshToken());

        String entityId = pbRefreshTokenEntity.getEntityId();
        //ADD Validation
        PbUserEntity pbUserEntity = userService.getUserEntity(entityId);
        checkAccountStatus(pbUserEntity);

        // Rotate: revoke old, issue new
        pbRefreshTokenEntity.setStatus(TokenStatus.REVOKED);
        refreshTokenRepo.save(pbRefreshTokenEntity);

        String newAccessToken  = jwtUtil.generateAccessToken(pbUserEntity);
        String newRefreshToken = jwtUtil.generateRefreshToken(pbUserEntity);

        PbRefreshTokenEntity newRt = PbRefreshTokenEntity.builder()
                .refreshToken(newRefreshToken)
                .entityId(entityId)
                .status(TokenStatus.ACTIVE)
                .expiresAt(Instant.now().plusMillis(jwtUtil.getRefreshTokenExpiryMs()))
                .deviceId(pbRefreshTokenEntity.getDeviceId())
                .platform(pbRefreshTokenEntity.getPlatform())
                .build();
        refreshTokenRepo.save(newRt);

        return TokenRefreshResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .accessTokenExpiresIn(props.getJwt().getAccessTokenExpiryMs() / 1000)
                .build();
    }

    // ── Logout ────────────────────────────────────────────────────────

    @Override
    @Transactional
    @AuditAction("USER_LOGOUT")
    public void logout(String accessToken, HttpServletRequest httpReq) {
        String email = jwtUtil.extractEmail(accessToken);
        userRepo.findByEmail(email).ifPresent(user -> {
            // Revoke only tokens from this device
            String deviceId = RequestUtil.getDeviceId(httpReq);
            refreshTokenRepo.revokeAllByEntityId(user.getEntityId());
            log.info("User logged out: {} from deviceId={}", email, deviceId);
        });

    }



    @Override
    @Transactional
    @AuditAction("USER_LOGOUT_ALL")
    public void logoutAll(String accessToken) {
        String email = jwtUtil.extractEmail(accessToken);
        userRepo.findByEmail(email).ifPresent(user -> {
            tokenService.revokeAllUserTokens(user);
            log.info("All tokens revoked for: {}", email);
        });

    }

    // ── Forgot Password ───────────────────────────────────────────────

    @Override
    @Transactional
    @AuditAction("PASSWORD_RESET_REQUEST")
    public void forgotPassword(ForgotPasswordRequest req, HttpServletRequest httpReq) {
        // Always return 200 — never leak whether email exists (security)
        userRepo.findByEmail(req.getEmail().toLowerCase()).ifPresent(user -> {
            resetTokenRepo.revokeAllByEntityId(user.getEntityId());

            String rawToken = UUID.randomUUID().toString().replace("-", "");
            PbPasswordResetTokenEntity prt = PbPasswordResetTokenEntity.builder()
                    .token(rawToken)
                    .entityId(user.getEntityId())
                    .status(TokenStatus.ACTIVE)
                    .expiresAt(Instant.now().plusSeconds(
                            props.getPasswordReset().getTokenExpiryMinutes() * 60L))
                    .requestedFromIp(RequestUtil.getClientIp(httpReq))
                    .build();
            resetTokenRepo.save(prt);

            mailService.sendPasswordResetMail(user, rawToken);
            log.info("Password reset mail sent to: {}", user.getEmail());
        });
    }

    // ── Reset Password ────────────────────────────────────────────────

    @Override
    @Transactional
    @AuditAction("PASSWORD_RESET_COMPLETE")
    public void resetPassword(ResetPasswordRequest req) {
        if (!req.getNewPassword().equals(req.getConfirmPassword())) {
            throw new InvalidResetTokenException(); // reuse — avoid leaking field name
        }

        PbPasswordResetTokenEntity pbPasswordResetTokenEntity = resetTokenRepo.findByToken(req.getToken())
                .orElseThrow(InvalidResetTokenException::new);

        if (!pbPasswordResetTokenEntity.isValid()) throw new InvalidResetTokenException();

        String entityId = pbPasswordResetTokenEntity.getEntityId();
        /* Add Validation for pbUserEntity if not valid */
        PbUserEntity pbUserEntity = userService.getUserEntity(entityId);

        pbUserEntity.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        userRepo.save(pbUserEntity);

        pbPasswordResetTokenEntity.setStatus(TokenStatus.USED);
        resetTokenRepo.save(pbPasswordResetTokenEntity);

        tokenService.revokeAllUserTokens(pbUserEntity); // force re-login on all devices
        log.info("Password reset completed for userId={}", pbUserEntity.getEntityId());
    }

    // ── Private helpers ───────────────────────────────────────────────

    private AuthResponse buildAuthResponse(PbUserEntity pbUserEntity, HttpServletRequest req) {
        String accessToken = jwtUtil.generateAccessToken(pbUserEntity);
        PbRefreshTokenEntity pbRefreshTokenEntity = tokenService.createRefreshToken(pbUserEntity, req);
        PbUserSessionEntity pbUserSessionEntity = tokenService.createSessionToken(pbUserEntity, req);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(pbRefreshTokenEntity.getRefreshToken())
                .sessionToken(pbUserSessionEntity.getToken())
                .accessTokenExpiresIn(props.getJwt().getAccessTokenExpiryMs() / 1000)
                .refreshTokenExpiresIn(props.getJwt().getRefreshTokenExpiryMs() / 1000)
                .user(UserResponse.from(pbUserEntity))
                .build();
    }

    private void checkAccountStatus(PbUserEntity user) {
        if (user.getStatus() == UserStatus.SUSPENDED) throw new AccountSuspendedException();
        if (!user.isAccountNonLocked()) throw new AccountLockedException();
        if (!user.isEnabled()) throw new AuthenticationException("Account is not active");
    }

    private void handleFailedLogin(PbUserEntity pbUserEntity) {
        pbUserEntity.incrementFailedAttempts();
        int max = props.getSecurity().getMaxFailedAttempts();
        if (pbUserEntity.getFailedLoginAttempts() >= max) {
            pbUserEntity.setLockedUntil(Instant.now().plusSeconds(
                    props.getSecurity().getLockDurationMinutes() * 60L));
            pbUserEntity.setStatus(UserStatus.LOCKED);
            userRepo.save(pbUserEntity);
            mailService.sendAccountLockedMail(pbUserEntity);
            log.warn("Account locked after {} failed attempts: {}", max, pbUserEntity.getEmail());
            throw new AccountLockedException("Account locked after " + max + " failed attempts");
        }
        userRepo.save(pbUserEntity);
    }
}

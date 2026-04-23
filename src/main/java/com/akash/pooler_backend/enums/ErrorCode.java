package com.akash.pooler_backend.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Authentication & Authorisation
    INVALID_CREDENTIALS("AUTH-001", "Invalid email or password", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED("AUTH-002", "Token has expired", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID("AUTH-003", "Token is invalid or malformed", HttpStatus.UNAUTHORIZED),
    TOKEN_REVOKED("AUTH-004", "Token has been revoked", HttpStatus.UNAUTHORIZED),
    TOKEN_NOT_FOUND("AUTH-005", "Token not found", HttpStatus.NOT_FOUND),
    REFRESH_TOKEN_EXPIRED("AUTH-006", "Refresh token has expired", HttpStatus.UNAUTHORIZED),
    SESSION_EXPIRED("AUTH-007", "Session has expired", HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED("AUTH-008", "Access denied - insufficient permissions", HttpStatus.FORBIDDEN),
    ACCOUNT_LOCKED("AUTH-009", "Account is locked", HttpStatus.FORBIDDEN),
    ACCOUNT_SUSPENDED("AUTH-010", "Account is suspended", HttpStatus.FORBIDDEN),
    ACCOUNT_INACTIVE("AUTH-011", "Account is not active", HttpStatus.FORBIDDEN),
    MISSING_AUTH_HEADER("AUTH-012","Authorization header is missing",HttpStatus.UNAUTHORIZED),

    // User
    USER_NOT_FOUND("USR-001", "User not found", HttpStatus.NOT_FOUND),
    USER_ALREADY_EXISTS("USR-002", "User with this email already exists", HttpStatus.CONFLICT),
    USER_NOT_VERIFIED("USR-003", "User email is not verified", HttpStatus.FORBIDDEN),

    // Password
    INVALID_RESET_TOKEN("PWD-001", "Password reset token is invalid or expired", HttpStatus.BAD_REQUEST),
    PASSWORD_MISMATCH("PWD-002", "Passwords do not match", HttpStatus.BAD_REQUEST),
    SAME_PASSWORD("PWD-003", "New password must be different from current password", HttpStatus.BAD_REQUEST),

    // Validation
    VALIDATION_ERROR("VAL-001", "Validation failed", HttpStatus.BAD_REQUEST),
    INVALID_REQUEST("VAL-002", "Invalid request body", HttpStatus.BAD_REQUEST),

    // Rate Limiting
    RATE_LIMIT_EXCEEDED("RATE-001", "Too many requests. Please try again later.", HttpStatus.TOO_MANY_REQUESTS),

    // Mail
    MAIL_SEND_FAILED("MAIL-001", "Failed to send email", HttpStatus.INTERNAL_SERVER_ERROR),

    // Generic
    INTERNAL_ERROR("SYS-001", "An internal server error occurred", HttpStatus.INTERNAL_SERVER_ERROR),
    RESOURCE_NOT_FOUND("SYS-002", "Requested resource not found", HttpStatus.NOT_FOUND);


    private final String code;
    private final String defaultMessage;
    private final HttpStatus httpStatus;
}

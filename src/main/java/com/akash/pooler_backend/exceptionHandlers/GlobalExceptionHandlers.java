package com.akash.pooler_backend.exceptionHandlers;

import com.akash.pooler_backend.dto.response.ApiResponse;
import com.akash.pooler_backend.enums.ErrorCode;
import com.akash.pooler_backend.exception.BaseException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.nio.file.AccessDeniedException;
import java.util.HashMap;
import java.util.Map;

/**
 * Centralised exception handler following the Single Responsibility Principle.
 * All exception-to-HTTP response mapping lives here.
 * @author Akash Kumar
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandlers {

    // ─────────────────────────────────────────────────────────────
    // Domain Exceptions (BaseException subtypes)
    // ─────────────────────────────────────────────────────────────

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<Void>> handleBaseException(
            BaseException ex, HttpServletRequest request) {
        log.warn("Domain exception [{}] on {}", ex.getErrorCode().getCode(), request.getRequestURI());
        return buildResponse(ex.getErrorCode(), ex.getMessage(), request);
    }

    // ─────────────────────────────────────────────────────────────
    // Spring Security Exceptions
    // ─────────────────────────────────────────────────────────────

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied on {}", request.getRequestURI());
        return buildResponse(ErrorCode.ACCESS_DENIED, ex.getMessage(), request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest request) {
        return buildResponse(ErrorCode.INVALID_CREDENTIALS, ex.getMessage(), request);
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ApiResponse<Void>> handleLocked(LockedException ex, HttpServletRequest request) {
        return buildResponse(ErrorCode.ACCOUNT_LOCKED, ex.getMessage(), request);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiResponse<Void>> handleDisabled(DisabledException ex, HttpServletRequest request) {
        return buildResponse(ErrorCode.ACCOUNT_INACTIVE, ex.getMessage(), request);
    }

    // ─────────────────────────────────────────────────────────────
    // Validation Exceptions
    // ─────────────────────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        log.warn("Validation failed on {} fields={}", request.getRequestURI(), fieldErrors.keySet());

        ApiResponse<Map<String, String>> response = ApiResponse.<Map<String, String>>builder()
                .success(false)
                .errorCode(ErrorCode.VALIDATION_ERROR.getCode())
                .message(ErrorCode.VALIDATION_ERROR.getDefaultMessage())
                .data(fieldErrors)
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        return buildResponse(ErrorCode.VALIDATION_ERROR, ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return buildResponse(ErrorCode.INVALID_REQUEST, "Invalid parameter type: " + ex.getName(), request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableBody(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("Invalid request body on {}", request.getRequestURI());
        return buildResponse(ErrorCode.INVALID_REQUEST, "Invalid request body", request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Invalid request on {}", request.getRequestURI());
        return buildResponse(ErrorCode.INVALID_REQUEST, ex.getMessage(), request);
    }

    // ─────────────────────────────────────────────────────────────
    // Catch-All
    // ─────────────────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {} type={}", request.getRequestURI(), ex.getClass().getSimpleName());
        return buildResponse(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.getDefaultMessage(), request);
    }

    // ─────────────────────────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────────────────────────

    private ResponseEntity<ApiResponse<Void>> buildResponse(ErrorCode errorCode, String message, HttpServletRequest request) {
        ApiResponse<Void> body = ApiResponse.<Void>builder()
                .success(false)
                .errorCode(errorCode.getCode())
                .message(message != null ? message : errorCode.getDefaultMessage())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(errorCode.getHttpStatus()).body(body);
    }


}

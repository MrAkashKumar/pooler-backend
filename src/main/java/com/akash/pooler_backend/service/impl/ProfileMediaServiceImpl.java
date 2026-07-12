package com.akash.pooler_backend.service.impl;

import com.akash.pooler_backend.config.ProfileMediaProperties;
import com.akash.pooler_backend.dto.response.UserResponse;
import com.akash.pooler_backend.entity.PbUserEntity;
import com.akash.pooler_backend.enums.ErrorCode;
import com.akash.pooler_backend.enums.ProfileMediaPurpose;
import com.akash.pooler_backend.exception.FileUploadException;
import com.akash.pooler_backend.interceptors.annotation.AuditAction;
import com.akash.pooler_backend.repository.PbUserRepository;
import com.akash.pooler_backend.service.ProfileMediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileMediaServiceImpl implements ProfileMediaService {

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final PbUserRepository userRepository;
    private final ProfileMediaProperties properties;
    private final S3Client profileMediaS3Client;

    @Override
    @AuditAction("PROFILE_MEDIA_UPLOAD")
    public UserResponse uploadProfileMedia(PbUserEntity user, ProfileMediaPurpose purpose, MultipartFile file) {
        validate(file);
        if (properties.getS3Bucket() == null || properties.getS3Bucket().isBlank()) {
            throw new FileUploadException(ErrorCode.VALIDATION_ERROR, "S3 bucket is not configured");
        }
        if (properties.getS3Region() == null || properties.getS3Region().isBlank()) {
            throw new FileUploadException(ErrorCode.VALIDATION_ERROR, "S3 region is not configured");
        }
        if (properties.getKeyPrefix() == null || properties.getKeyPrefix().isBlank()) {
            throw new FileUploadException(ErrorCode.VALIDATION_ERROR, "S3 key prefix is not configured");
        }

        String contentType = file.getContentType() == null ? "application/octet-stream" : file.getContentType();
        String key = buildKey(user.getEntityId(), purpose, contentType);
        try {
            profileMediaS3Client.putObject(PutObjectRequest.builder()
                            .bucket(properties.getS3Bucket())
                            .key(key)
                            .contentType(contentType)
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (IOException exception) {
            throw new FileUploadException("Could not read uploaded profile media");
        } catch (RuntimeException exception) {
            throw new FileUploadException("Could not upload profile media to S3");
        }

        String mediaUrl = publicUrl(key);
        if (purpose == ProfileMediaPurpose.PROFILE_PHOTO) {
            user.setProfilePictureUrl(mediaUrl);
        } else {
            user.setPaymentQrCodeUrl(mediaUrl);
        }
        return UserResponse.from(userRepository.save(user));
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileUploadException(ErrorCode.VALIDATION_ERROR, "Profile media file is required");
        }
        if (file.getSize() > properties.getMaxSizeMb() * 1024 * 1024) {
            throw new FileUploadException(ErrorCode.VALIDATION_ERROR,
                    "Profile media must be %d MB or smaller".formatted(properties.getMaxSizeMb()));
        }
        String contentType = file.getContentType() == null ? "application/octet-stream" : file.getContentType();
        if (!ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new FileUploadException(ErrorCode.VALIDATION_ERROR, "Only JPEG, PNG, and WebP images are allowed");
        }
    }

    private String buildKey(String userEntityId, ProfileMediaPurpose purpose, String contentType) {
        String extension = switch (contentType) {
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> "jpg";
        };
        return "%s/%s/%s/%s.%s".formatted(
                trimSlashes(properties.getKeyPrefix()),
                userEntityId,
                purpose.name().toLowerCase(Locale.ROOT).replace('_', '-'),
                UUID.randomUUID().toString().replace("-", ""),
                extension);
    }

    private String publicUrl(String key) {
        if (properties.getPublicBaseUrl() != null && !properties.getPublicBaseUrl().isBlank()) {
            return trimTrailingSlash(properties.getPublicBaseUrl()) + "/" + key;
        }
        return "https://%s.s3.%s.amazonaws.com/%s".formatted(
                properties.getS3Bucket(),
                properties.getS3Region(),
                encodeKey(key));
    }

    private static String encodeKey(String key) {
        return URLEncoder.encode(key, StandardCharsets.UTF_8).replace("+", "%20").replace("%2F", "/");
    }

    private static String trimSlashes(String value) {
        String trimmed = value.trim();
        trimmed = trimmed.replaceAll("^/+", "").replaceAll("/+$", "");
        return trimmed;
    }

    private static String trimTrailingSlash(String value) {
        return value.replaceAll("/+$", "");
    }
}

package com.akash.pooler_backend.service.impl;

import com.akash.pooler_backend.constants.ResponseMessages;
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
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class ProfileMediaServiceImpl implements ProfileMediaService {

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final String METHOD_UPLOAD_PROFILE_MEDIA = "uploadProfileMedia";

    private final PbUserRepository userRepository;
    private final ProfileMediaProperties properties;
    private final S3Client profileMediaS3Client;

    @Override
    @AuditAction("PROFILE_MEDIA_UPLOAD")
    public UserResponse uploadProfileMedia(PbUserEntity user, ProfileMediaPurpose purpose, MultipartFile file) {
        validate(file);
        if (properties.getS3Bucket() == null || properties.getS3Bucket().isBlank()) {
            throw new FileUploadException(ErrorCode.VALIDATION_ERROR, ResponseMessages.S3_BUCKET_NOT_CONFIGURED);
        }
        if (properties.getS3Region() == null || properties.getS3Region().isBlank()) {
            throw new FileUploadException(ErrorCode.VALIDATION_ERROR, ResponseMessages.S3_REGION_NOT_CONFIGURED);
        }
        if (properties.getKeyPrefix() == null || properties.getKeyPrefix().isBlank()) {
            throw new FileUploadException(ErrorCode.VALIDATION_ERROR, ResponseMessages.S3_KEY_PREFIX_NOT_CONFIGURED);
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
            log.error("profileMediaReadFailed className={} methodName={} userId={} purpose={} exceptionType={}",
                    getClass().getSimpleName(), METHOD_UPLOAD_PROFILE_MEDIA, user.getEntityId(), purpose,
                    exception.getClass().getSimpleName(), exception);
            throw new FileUploadException(ResponseMessages.PROFILE_MEDIA_READ_FAILED);
        } catch (RuntimeException exception) {
            log.error("profileMediaUploadFailed className={} methodName={} userId={} purpose={} bucket={} keyPrefix={} exceptionType={}",
                    getClass().getSimpleName(), METHOD_UPLOAD_PROFILE_MEDIA, user.getEntityId(), purpose,
                    properties.getS3Bucket(), properties.getKeyPrefix(), exception.getClass().getSimpleName(), exception);
            throw new FileUploadException(ResponseMessages.PROFILE_MEDIA_S3_UPLOAD_FAILED);
        }

        String mediaUrl = publicUrl(key);
        if (purpose == ProfileMediaPurpose.PROFILE_PHOTO) {
            user.setProfilePictureUrl(mediaUrl);
        } else {
            user.setPaymentQrCodeUrl(mediaUrl);
        }
        log.info("profileMediaUploaded className={} methodName={} userId={} purpose={} contentType={} sizeBytes={} key={}",
                getClass().getSimpleName(), METHOD_UPLOAD_PROFILE_MEDIA, user.getEntityId(), purpose, contentType, file.getSize(), key);
        return UserResponse.from(userRepository.save(user));
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileUploadException(ErrorCode.VALIDATION_ERROR, ResponseMessages.PROFILE_MEDIA_REQUIRED);
        }
        if (file.getSize() > properties.getMaxSizeMb() * 1024 * 1024) {
            throw new FileUploadException(ErrorCode.VALIDATION_ERROR,
                    ResponseMessages.profileMediaMaxSize(properties.getMaxSizeMb()));
        }
        String contentType = file.getContentType() == null ? "application/octet-stream" : file.getContentType();
        if (!ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new FileUploadException(ErrorCode.VALIDATION_ERROR, ResponseMessages.PROFILE_MEDIA_IMAGE_ONLY);
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

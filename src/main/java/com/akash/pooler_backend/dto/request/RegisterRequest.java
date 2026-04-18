package com.akash.pooler_backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author Akash Kumar
 */
@Data
public class RegisterRequest {

    @NotBlank(message="First name is required")
    @Size(min=2,max=100,message="First name must be 2-100 characters")
    private String firstName;

    @NotBlank(message="Last name is required")
    @Size(min=2,max=100, message="Last name must be 2-100 characters")
    private String lastName;

    @NotBlank(message="Email is required")
    @Email(message="Must be a valid email address")
    private String email;

    @NotBlank(message="Password is required")
    @Size(min=8,max=72, message="Password must be 8-72 characters")
    @Pattern(regexp="^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).+$",
            message="Password must contain uppercase, lowercase, digit and special character")
    private String password;
    /**
     * Mobile device metadata
      */
    private String deviceId;
    /**
     * ANDROID | IOS
     *  */
    private String platform;
    private String appVersion;
    /**
     * Firebase push notification token
     */
    private String fcmToken;
}

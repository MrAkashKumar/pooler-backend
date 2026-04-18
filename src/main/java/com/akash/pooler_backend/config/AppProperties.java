package com.akash.pooler_backend.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Type-safe binding of all `app.*` properties.
 * Fails fast at startup if required properties are missing.
 */
@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    @NotBlank
    private String name;

    @NotBlank
    private String version;

    @NotBlank
    private String baseUrl;

    @NotNull
    private Jwt jwt = new Jwt();

    @NotNull
    private Security security = new Security();

    @NotNull
    private Mail mail = new Mail();

    @NotNull
    private PasswordReset passwordReset = new PasswordReset();

    // ─── Nested config classes ────────────────────────────────────

    @Getter @Setter
    public static class Jwt {
        @NotBlank
        private String secret;
        @Positive
        private long accessTokenExpiryMs  = 900_000L;       // 15 min
        @Positive
        private long refreshTokenExpiryMs = 604_800_000L;   // 7 days
        @Positive
        private long sessionTokenExpiryMs = 1_800_000L;     // 30 min
        @NotBlank
        private String issuer   = "enterprise-auth";
        @NotBlank
        private String audience = "enterprise-mobile-app";
    }

    @Getter @Setter
    public static class Security {
        private int maxFailedAttempts    = 5;
        private int lockDurationMinutes  = 30;
        private int bcryptStrength       = 12;
        private Cors cors = new Cors();

        @Getter
        @Setter
        public static class Cors {
            private String allowedOrigins = "*";
            private String allowedMethods = "GET,POST,PUT,DELETE,OPTIONS,PATCH";
        }
    }

    @Getter
    @Setter
    public static class Mail {
        @NotBlank
        private String from = "noreply@enterprise.com";
        private String fromName = "Enterprise Auth";
    }

    @Getter @Setter
    public static class PasswordReset {
        private int tokenExpiryMinutes = 30;
        private int maxAttempts        = 3;
    }
}

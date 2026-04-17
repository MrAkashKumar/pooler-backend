package com.akash.pooler_backend.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * @author Akash Kumar
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title       = "Pooler Auth API",
                version     = "1.0.0",
                description = """
                JWT-based authentication service for Android/iOS mobile backends.
                
                **Auth flow:**
                1. `POST /api/v1/auth/register` — create account
                2. `POST /api/v1/auth/login` — get access + refresh tokens
                3. Attach `Authorization: Bearer <access_token>` to all requests
                4. `POST /api/v1/auth/refresh` — exchange refresh token for new access token
                5. `POST /api/v1/auth/logout` — revoke tokens
                """,
                contact = @Contact(name = "Enterprise Team", email = "dev@enterprise.com"),
                license = @License(name = "MIT", url = "https://opensource.org/licenses/MIT")
        ),
        servers = {
                @Server(url = "http://localhost:8080", description = "Local Dev"),
                @Server(url = "https://staging.pooler.com", description = "Staging"),
                @Server(url = "https://api.pooler.com", description = "Production")
        }
)
@SecurityScheme(
        name        = "bearerAuth",
        type        = SecuritySchemeType.HTTP,
        scheme      = "bearer",
        bearerFormat= "JWT",
        in          = SecuritySchemeIn.HEADER,
        description = "Paste your access token here. Obtain it from /api/v1/auth/login"
)
public class OpenApiConfig {
}

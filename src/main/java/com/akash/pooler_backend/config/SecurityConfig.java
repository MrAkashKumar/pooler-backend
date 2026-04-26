package com.akash.pooler_backend.config;

import com.akash.pooler_backend.security.CustomAccessDeniedHandler;
import com.akash.pooler_backend.security.CustomAuthEntryPoint;
import com.akash.pooler_backend.security.filter.RequestLoggingFilter;
import com.akash.pooler_backend.security.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  SecurityConfig — Central Spring Security Configuration          │
 * │                                                                  │
 * │  Design decisions:                                               │
 * │  ① STATELESS session — all state lives in JWT tokens           │
 * │  ② CSRF disabled — not needed for stateless REST + mobile      │
 * │  ③ Dual-layer auth: JWT filter + AuthInterceptor (session DB)  │
 * │  ④ Custom 401/403 JSON handlers for mobile clients             │
 * │  ⑤ CORS configured for Android/iOS origins                    │
 * │  ⑥ @PreAuthorize + @RequiresAuth for method-level security     │
 * │                                                                  │
 * │  Filter execution order:                                         │
 * │  RequestLoggingFilter → JwtAuthenticationFilter                 │
 * │       → UsernamePasswordAuthenticationFilter (unused)           │
 * │       → SecurityFilterChain rules                               │
 * │       → AuthInterceptor (MVC HandlerInterceptor)               │
 * │       → Controller                                              │
 * └─────────────────────────────────────────────────────────────────┘
 */

/**
 * @author Akash kumar
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) /* enables @PreAuthorize / @PostAuthorize */
@RequiredArgsConstructor
public class SecurityConfig {

    // ─── Injected dependencies ─────────────────────────────────────────
    private final JwtAuthenticationFilter jwtAuthFilter;       // validates JWT on every request
    private final RequestLoggingFilter requestLoggingFilter; // structured request/response logs
    private final UserDetailsService userDetailsService;  // loads User entity from DB
    private final CustomAuthEntryPoint authEntryPoint;      // 401 JSON response
    private final CustomAccessDeniedHandler accessDeniedHandler; // 403 JSON response
    private final AppProperties appProps;

    // ─── Public routes (no token required) ────────────────────────────
    private static final String[] PUBLIC_MATCHERS = {
            // Auth lifecycle
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            // Public info
            "/api/v1/public/**",
            // API documentation
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            // Dev tooling (H2 — gated by profile in application-prod.yml)
            "/h2-console/**",
            // Monitoring
            "/actuator/health",
            "/actuator/info"
    };

    // ─── Main security filter chain ────────────────────────────────────
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // ① Disable CSRF — stateless REST API, no cookie sessions
                .csrf(AbstractHttpConfigurer::disable)

                // ② CORS — configured for mobile client origins
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // ③ No HttpSession — every request must carry a JWT
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // ④ Custom error handlers for mobile JSON clients
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authEntryPoint)   // 401
                        .accessDeniedHandler(accessDeniedHandler))  // 403

                // ⑤ Authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Public — no token needed
                        .requestMatchers(PUBLIC_MATCHERS).permitAll()
                        // CORS preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Role-restricted admin routes
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/moderator/**").hasAnyRole("ADMIN", "MODERATOR")
                        // Everything else requires a valid JWT
                        .anyRequest().authenticated())

                // ⑥ Allow H2 console iframes (dev only — disabled in prod via config)
                .headers(headers ->
                        headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))

                // ⑦ Wire DaoAuthenticationProvider for login
                .authenticationProvider(authenticationProvider())

                // ⑧ Filter chain order:
                //    1st — RequestLoggingFilter (attaches correlation ID, logs request)
                //    2nd — JwtAuthenticationFilter (validates token, sets SecurityContext)
                .addFilterBefore(requestLoggingFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

                .build();
    }

    // ─── Authentication provider ───────────────────────────────────────

    /**
     * DaoAuthenticationProvider — Spring Security's standard username/password
     * authenticator backed by our UserDetailsServiceImpl + BCrypt.
     * Used during POST /api/v1/auth/login.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        // Do NOT hide UserNotFoundException — we handle generic messages ourselves
        provider.setHideUserNotFoundExceptions(false);
        return provider;
    }

    /**
     * Exposes AuthenticationManager as a bean so AuthServiceImpl can inject it
     * and call authManager.authenticate() during login.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * BCrypt password encoder.
     * Strength is profile-driven:
     *   dev/test → 4 (fast)
     *   staging   → 10
     *   prod      → 12 (OWASP recommended minimum)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(appProps.getSecurity().getBcryptStrength());
    }

    // ─── CORS configuration ────────────────────────────────────────────

    /**
     * CORS policy for Android/iOS clients.
     *
     * Key decisions:
     * - allowedOriginPatterns("*") in dev; restrict to real origins in prod
     * - Mobile-specific headers explicitly allowed (X-Device-Id, X-Platform, etc.)
     * - No allowCredentials (incompatible with wildcard origins; we use Bearer tokens)
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        String originsProperty = appProps.getSecurity().getCors().getAllowedOrigins();
        String methodsProperty = appProps.getSecurity().getCors().getAllowedMethods();

        // Origin policy
        if ("*".equals(originsProperty)) {
            config.setAllowedOriginPatterns(List.of("*"));
        } else {
            config.setAllowedOrigins(Arrays.asList(originsProperty.split(",")));
        }

        // HTTP methods
        config.setAllowedMethods(Arrays.asList(methodsProperty.split(",")));

        // Standard + mobile-specific headers
        config.setAllowedHeaders(List.of(
                "Authorization",         // Bearer <token>
                "Content-Type",          // application/json
                "Accept",
                "X-Requested-With",
                "X-Device-Id",           // Unique device identifier
                "X-Platform",            // ANDROID | IOS | WEB
                "X-App-Version",         // semver app version
                "X-FCM-Token",           // Firebase push token
                "X-Session-Token",       // Server-side session token
                "X-Correlation-ID"       // Request tracing
        ));

        // Headers visible to mobile client JS/Kotlin
        config.setExposedHeaders(List.of(
                "Authorization",
                "X-Correlation-ID",
                "X-Refresh-Token"
        ));

        config.setAllowCredentials(false); // Must be false with wildcard origins
        config.setMaxAge(3600L);           // Cache CORS preflight for 1 hour

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}

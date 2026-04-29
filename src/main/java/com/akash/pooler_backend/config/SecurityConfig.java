package com.akash.pooler_backend.config;

import com.akash.pooler_backend.security.CustomAccessDeniedHandler;
import com.akash.pooler_backend.security.CustomAuthEntryPoint;
import com.akash.pooler_backend.security.filter.RequestLoggingFilter;
import com.akash.pooler_backend.security.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.security.autoconfigure.web.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
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
 *
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
    private final JwtAuthenticationFilter jwtAuthFilter;
    private final RequestLoggingFilter requestLoggingFilter;
    private final UserDetailsService userDetailsService;
    private final CustomAuthEntryPoint authEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final AppProperties appProps;

    // ─── Public routes (no token required) ────────────────────────────
    private static final String[] PUBLIC_MATCHERS = {
            // Auth lifecycle
            "/auth/register",
            "/auth/login",
            "/auth/refresh",
            "/auth/forgot-password",
            "/auth/reset-password",
            // Public info
            "/public/**",
            // API documentation
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            // Monitoring
            "/actuator/health",
            "/actuator/info"
    };

    // ─── Main security filter chain ────────────────────────────────────
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                //  Disable CSRF — stateless REST API, no cookie sessions
                .csrf(AbstractHttpConfigurer::disable)

                //  CORS — configured for mobile client origins
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                //  No HttpSession — every request must carry a JWT
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                //  Custom error handlers for mobile JSON clients
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authEntryPoint)   // 401
                        .accessDeniedHandler(accessDeniedHandler))  // 403

                //  Authorization rules
                .authorizeHttpRequests(auth -> auth
                        //  PathRequest.toH2Console() — works correctly for H2 servlet
                        // in Spring Security 6; MvcRequestMatcher("/h2-console/**") does NOT.
                        .requestMatchers(PathRequest.toH2Console()).permitAll()
                        // Public — no token needed
                        .requestMatchers(PUBLIC_MATCHERS).permitAll()
                        // CORS preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Role-restricted admin routes
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/moderator/**").hasAnyRole("ADMIN", "MODERATOR")
                        // Everything else requires a valid JWT
                        .anyRequest().authenticated())

                //     Allow H2 console iframes — sameOrigin needed so H2's embedded
                //    iframe can render inside the same browser origin. The matcher
                //    mirrors ⑤ above so the header rule covers exactly the same paths.
                .headers(headers ->
                        headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))

                // AuthenticationProvider is NOT a @Bean anymore.
                //    It is wired directly into the ProviderManager inside
                //    authenticationManager() below. This eliminates the Spring Boot WARN:
                //    "Global AuthenticationManager configured with an AuthenticationProvider
                //     bean. UserDetailsService beans will not be used…"
                .authenticationManager(authenticationManager())

                // ⑧ Filter chain order:
                //    1st — RequestLoggingFilter (attaches correlation ID, logs request)
                //    2nd — JwtAuthenticationFilter (validates token, sets SecurityContext)
                .addFilterBefore(requestLoggingFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthFilter, RequestLoggingFilter.class)

                .build();
    }

    // ─── Authentication manager ────────────────────────────────────────

    /**
     * This bean is still injectable by AuthServiceImpl via @Autowired /
     * constructor injection as it is a @Bean here.
     */
    @Bean
    public AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        // Do NOT hide UserNotFoundException — we handle generic messages ourselves
        provider.setHideUserNotFoundExceptions(false);
        return new ProviderManager(provider);
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
                "Authorization",
                "Content-Type",
                "Accept",
                "X-Requested-With",
                "X-Device-Id",
                "X-Platform",
                "X-App-Version",
                "X-FCM-Token",
                "X-Session-Token",
                "X-Correlation-ID"
        ));

        // Headers visible to mobile client JS/Kotlin
        config.setExposedHeaders(List.of(
                "Authorization",
                "X-Correlation-ID",
                "X-Refresh-Token"
        ));

        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}

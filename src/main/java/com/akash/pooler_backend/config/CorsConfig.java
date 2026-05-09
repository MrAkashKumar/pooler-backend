package com.akash.pooler_backend.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Global CORS configuration that runs BEFORE Spring Security.
 *
 * This ensures:
 * 1. Swagger UI can fetch /v3/api-docs without CORS errors
 * 2. Preflight OPTIONS requests are handled correctly
 * 3. All API endpoints allow cross-origin requests from configured origins
 *
 * @author Akash kumar
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class CorsConfig {

    private final AppProperties appProperties;

    /**
     * CORS filter registered with highest precedence to run before Spring Security.
     */
    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilterRegistrationBean() {
        CorsConfiguration config = new CorsConfiguration();

        String originsProperty = appProperties.getSecurity().getCors().getAllowedOrigins();
        String methodsProperty = appProperties.getSecurity().getCors().getAllowedMethods();

        // Configure allowed origins
        if ("*".equals(originsProperty)) {
            // For development - allow all origins via pattern
            config.setAllowedOriginPatterns(List.of("*"));
            log.info("CORS: Allowing all origins (development mode)");
        } else {
            List<String> origins = Arrays.asList(originsProperty.split(","));
            config.setAllowedOrigins(origins);
            log.info("CORS: Allowing specific origins: {}", origins);
        }

        // Configure allowed methods
        config.setAllowedMethods(Arrays.asList(methodsProperty.split(",")));

        // Configure allowed headers - be generous for API clients
        config.setAllowedHeaders(List.of(
                "*"  // Allow all headers for simplicity
        ));

        // Expose headers that clients may need to read
        config.setExposedHeaders(List.of(
                "Authorization",
                "X-Correlation-ID",
                "X-Refresh-Token",
                "Content-Disposition",
                "X-Total-Count",
                "X-Page-Number",
                "X-Page-Size"
        ));

        // Allow credentials (cookies, authorization headers)
        // Note: When allowCredentials is true, allowedOrigins cannot contain "*"
        if (!"*".equals(originsProperty)) {
            config.setAllowCredentials(true);
        } else {
            config.setAllowCredentials(false);
        }

        // Cache preflight response for 1 hour
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Apply CORS configuration to ALL paths including swagger
        source.registerCorsConfiguration("/**", config);

        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(source));
        // Run BEFORE Spring Security filter chain
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        bean.setName("corsFilter");

        log.info("CORS filter registered with highest precedence");
        return bean;
    }

    /**
     * Additional filter to handle CORS preflight for paths that might bypass the CorsFilter.
     * This is a fallback for edge cases.
     */
    @Bean
    public FilterRegistrationBean<Filter> additionalCorsFilter() {
        FilterRegistrationBean<Filter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new Filter() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                    throws IOException, ServletException {
                HttpServletRequest req = (HttpServletRequest) request;
                HttpServletResponse res = (HttpServletResponse) response;

                String origin = req.getHeader("Origin");
                if (origin != null) {
                    String originsProperty = appProperties.getSecurity().getCors().getAllowedOrigins();
                    if ("*".equals(originsProperty) || originsProperty.contains(origin)) {
                        res.setHeader("Access-Control-Allow-Origin", origin);
                        res.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, PATCH, OPTIONS, HEAD");
                        res.setHeader("Access-Control-Allow-Headers", "*");
                        res.setHeader("Access-Control-Expose-Headers", "Authorization, X-Correlation-ID, X-Refresh-Token");
                        res.setHeader("Access-Control-Max-Age", "3600");

                        if (!"*".equals(originsProperty)) {
                            res.setHeader("Access-Control-Allow-Credentials", "true");
                        }
                    }
                }

                // Handle preflight
                if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
                    res.setStatus(HttpServletResponse.SC_OK);
                    return;
                }

                chain.doFilter(request, response);
            }
        });
        bean.addUrlPatterns("/*");
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        bean.setName("additionalCorsFilter");
        return bean;
    }
}
package com.akash.pooler_backend.config;

import com.akash.pooler_backend.aspect.CurrentUserArgumentResolver;
import com.akash.pooler_backend.aspect.DeviceInfoArgumentResolver;
import com.akash.pooler_backend.interceptors.AuthInterceptor;
import com.akash.pooler_backend.interceptors.RequestMetadataInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.tomcat.TomcatProtocolHandlerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * @author Akash kumar
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final RequestMetadataInterceptor requestMetadataInterceptor;
    private final CurrentUserArgumentResolver currentUserResolver;
    private final DeviceInfoArgumentResolver deviceInfoResolver;
    private final AppProperties appProperties;

    /**
     * Configure CORS at the WebMvc level as an additional layer.
     * This works alongside CorsConfig and SecurityConfig for comprehensive CORS support.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String originsProperty = appProperties.getSecurity().getCors().getAllowedOrigins();
        String methodsProperty = appProperties.getSecurity().getCors().getAllowedMethods();

        String[] origins = "*".equals(originsProperty)
                ? new String[]{"*"}
                : Arrays.stream(originsProperty.split(",")).map(String::trim).toArray(String[]::new);

        String[] methods = methodsProperty.split(",");

        registry.addMapping("/**")
                .allowedOriginPatterns(origins)
                .allowedMethods(methods)
                .allowedHeaders("*")
                .exposedHeaders("Authorization", "X-Correlation-ID", "X-Refresh-Token")
                .allowCredentials(!"*".equals(originsProperty))
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requestMetadataInterceptor).addPathPatterns("/**");
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/v1/auth/**","/api/v1/public/**",
                        "/v3/api-docs/**","/swagger-ui/**","/h2-console/**");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserResolver);
        resolvers.add(deviceInfoResolver);
    }

    @Bean
    public TomcatProtocolHandlerCustomizer<?> virtualThreadsCustomizer() {
        return handler -> handler.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
    }
}

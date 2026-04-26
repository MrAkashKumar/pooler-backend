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
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

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

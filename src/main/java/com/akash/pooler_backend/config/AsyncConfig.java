package com.akash.pooler_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async configuration for background tasks (e.g. mail dispatch).
 * Uses virtual threads on JDK 21 for the async executor.
 */
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    @Bean("mailExecutor")
    public Executor mailExecutor() {
        // JDK 21 virtual thread executor — no pool tuning needed
        return Thread.ofVirtual().name("mail-vt-", 0).factory()::newThread;
    }

    @Bean("defaultExecutor")
    public Executor defaultExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }


}

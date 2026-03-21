package com.kronohealth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Dedicated async thread pool for AI document analysis tasks.
 * Isolated from the default Spring async executor to prevent
 * long-running AI calls from blocking other async operations.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Value("${app.analysis.async.core-pool-size:2}")
    private int corePoolSize;

    @Value("${app.analysis.async.max-pool-size:5}")
    private int maxPoolSize;

    @Value("${app.analysis.async.queue-capacity:100}")
    private int queueCapacity;

    @Bean(name = "aiAnalysisExecutor")
    public Executor aiAnalysisExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("ai-analysis-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(120);
        executor.initialize();
        return executor;
    }
}


package com.diploma.analysis_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class ExecutorConfig {

    @Bean(destroyMethod = "shutdown")
    public ExecutorService analysisExecutor(@Value("${app.analysis.parallelism:4}") int parallelism) {
        ThreadFactory factory = new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger();

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "analysis-runner-" + counter.incrementAndGet());
                t.setDaemon(true);
                return t;
            }
        };
        return Executors.newFixedThreadPool(parallelism, factory);
    }
}

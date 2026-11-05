package com.automation.flowmusic.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.scheduling.annotation.EnableAsync;

@Slf4j
@Configuration
@EnableAsync
@EnableConfigurationProperties(AutomationProperties.class)
public class AsyncConfig {

    public static final String BATCH_QUEUE_EXECUTOR = "batchQueueExecutor";
    public static final String VIRTUAL_THREAD_EXECUTOR = "virtualThreadExecutor";

    /**
     * Executor geral baseado em Virtual Threads (Java 21).
     */
    @Bean(name = VIRTUAL_THREAD_EXECUTOR, destroyMethod = "close")
    public ExecutorService virtualThreadExecutorService() {
        ThreadFactory factory = Thread.ofVirtual().name("flow-vt-", 0).factory();
        return Executors.newThreadPerTaskExecutor(factory);
    }

    /**
     * Executor single-consumer (fila sequencial) suportado por Virtual Thread,
     * garantindo que apenas 1 lote interaja com a aba do navegador por vez.
     */
    @Bean(name = "batchQueueExecutorService", destroyMethod = "close")
    public ExecutorService batchQueueExecutorService() {
        ThreadFactory factory = Thread.ofVirtual().name("batch-queue-vt-", 0).factory();
        return Executors.newSingleThreadExecutor(factory);
    }

    @Bean(name = {BATCH_QUEUE_EXECUTOR, "taskExecutor"})
    public AsyncTaskExecutor batchQueueExecutor(Executor batchQueueExecutorService) {
        return new TaskExecutorAdapter(batchQueueExecutorService);
    }

    @Bean
    public ApplicationRunner startupConfigLogger(AutomationProperties props, Environment env) {
        return args -> {
            boolean virtualThreads = env.getProperty("spring.threads.virtual.enabled", Boolean.class, false);
            log.info(
                    "Automation config -> cdpUrl={}, flowMusicUrl={}, jitter={}-{}ms, cooldown={}ms, retry={}x (backoff={}s), defaultTargetCount={}, minFileSize={}B, virtualThreads={}",
                    props.getBrowser().getCdpUrl(),
                    props.getBrowser().getFlowMusicUrl(),
                    props.getTiming().getJitterMinMs(),
                    props.getTiming().getJitterMaxMs(),
                    props.getTiming().getBatchCooldownMs(),
                    props.getRetry().getMaxAttempts(),
                    props.getRetry().getBackoffBaseSeconds(),
                    props.getGeneration().getDefaultTargetCount(),
                    props.getDownload().getMinFileSizeBytes(),
                    virtualThreads
            );
        };
    }
}


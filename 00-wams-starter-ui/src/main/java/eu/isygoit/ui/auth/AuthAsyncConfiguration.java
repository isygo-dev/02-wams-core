package eu.isygoit.ui.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
public class AuthAsyncConfiguration {

    @Bean(name = "authExecutor", destroyMethod = "shutdown")
    public ThreadPoolTaskExecutor authExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(12);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("auth-service-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }

    @Bean(name = "authPollScheduler", destroyMethod = "shutdown")
    public ScheduledExecutorService authPollScheduler() {
        return Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "auth-qr-poll");
            thread.setDaemon(true);
            return thread;
        });
    }
}

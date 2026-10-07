package eu.isygoit.async;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class ContextAwareAsyncConfig {

    @Bean(name = "contextAwareAsyncPool")
    public ThreadPoolTaskExecutor contextAwareAsyncPool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("ctx-async-");
        executor.setTaskDecorator(ContextAwareAsync::contextualize);
        return executor;
    }

    @Bean(name = "contextAwareAsyncExecutor")
    public AsyncTaskExecutor contextAwareAsyncExecutor(
            @Qualifier("contextAwareAsyncPool") ThreadPoolTaskExecutor executor) {
        AsyncTaskExecutor contextAwareExecutor = new DelegatingSecurityContextAsyncTaskExecutor(executor);
        ContextAwareAsync.setDefaultExecutor(contextAwareExecutor);
        return contextAwareExecutor;
    }
}

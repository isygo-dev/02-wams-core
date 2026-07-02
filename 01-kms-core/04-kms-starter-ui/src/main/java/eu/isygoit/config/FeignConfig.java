package eu.isygoit.config;

import eu.isygoit.security.interceptor.BearerTokenRequestInterceptor;
import feign.RequestInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;

/**
 * The type Feign config.
 */
@Slf4j
public class FeignConfig {

    @Bean
    public RequestInterceptor bearerTokenInterceptor() {
        return new BearerTokenRequestInterceptor();
    }
}
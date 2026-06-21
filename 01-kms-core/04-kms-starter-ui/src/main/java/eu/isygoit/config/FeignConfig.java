package eu.isygoit.config;

import eu.isygoit.jwt.JwtService;
import eu.isygoit.security.interceptor.BearerTokenRequestInterceptor;
import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

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
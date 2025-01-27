package eu.isygoit.config;

import eu.isygoit.jwt.JwtService;
import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Objects;

/**
 * The type Feign config.
 */
@Slf4j
public class FeignConfig {

    /**
     * Not filtered request interceptor request interceptor.
     *
     * @return the request interceptor
     */
    @Bean
    public RequestInterceptor notFilteredRequestInterceptor() {
        return requestTemplate -> {
            //requestTemplate.header("SHOULD_NOT_FILTER_KEY", AbstractJwtAuthFilter.SHOULD_NOT_FILTER_KEY);
            ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (Objects.isNull(requestAttributes)) {
                return;
            }
            HttpServletRequest request = requestAttributes.getRequest();
            if (Objects.isNull(request)) {
                return;
            }
            String jwtToken = request.getHeader(JwtService.AUTHORIZATION);
            if (!StringUtils.hasText(jwtToken)) {
                return;
            }

            requestTemplate.header(JwtService.AUTHORIZATION, jwtToken);
        };
    }
}
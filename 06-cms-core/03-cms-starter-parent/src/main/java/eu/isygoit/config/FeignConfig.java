package eu.isygoit.config;

import eu.isygoit.jwt.JwtService;
import feign.RequestInterceptor;
import feign.codec.Encoder;
import feign.form.spring.SpringFormEncoder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

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
            if (requestAttributes == null) {
                return;
            }
            HttpServletRequest request = requestAttributes.getRequest();
            if (request == null) {
                return;
            }
            String jwtToken = request.getHeader(JwtService.AUTHORIZATION);
            if (jwtToken == null) {
                return;
            }

            requestTemplate.header(JwtService.AUTHORIZATION, jwtToken);
        };
    }

    @Bean
    @Primary
    @Scope("prototype")
    public Encoder feignFormEncoder(ObjectFactory<HttpMessageConverters> messageConverters) {
        return new SpringFormEncoder(new SpringEncoder(messageConverters));
    }
}
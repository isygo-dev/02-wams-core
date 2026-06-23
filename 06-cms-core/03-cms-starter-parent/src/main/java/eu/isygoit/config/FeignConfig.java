package eu.isygoit.config;

import eu.isygoit.jwt.interceptor.SmartAuthInterceptor;
import feign.RequestInterceptor;
import feign.codec.Encoder;
import feign.form.spring.SpringFormEncoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;

/**
 * The type Feign config.
 */
@Slf4j
public class FeignConfig {

    @Value("${app.feign.api.internal-key:}")
    private String serviceApiKey;

    @Value("${spring.application.name:}")
    private String serviceId;

    @Bean
    public RequestInterceptor smartAuthInterceptor() {
        return new SmartAuthInterceptor(serviceApiKey, serviceId);
    }

    @Bean
    @Primary
    @Scope("prototype")
    public Encoder feignFormEncoder(ObjectFactory<HttpMessageConverters> messageConverters) {
        return new SpringFormEncoder(new SpringEncoder(messageConverters));
    }
}
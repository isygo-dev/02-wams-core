package eu.isygoit.config;

import eu.isygoit.i18n.helper.LocaleResolver;
import feign.codec.Encoder;
import feign.form.spring.SpringFormEncoder;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * The type App config.
 */
@Configuration
@EnableCaching //https://docs.spring.io/spring-framework/reference/integration/cache/annotations.html
@EnableConfigurationProperties(AppProperties.class)
public class AppConfig {

    /**
     * Gets local resolver.
     *
     * @return the local resolver
     */
    @Bean(name = "localResolver")
    public LocaleResolver getLocalResolver() {
        return new LocaleResolver();
    }

    /**
     * Gets extended message map.
     *
     * @return the extended message map
     */
    @Bean("extendedMessageMap")
    public Map<String, String> getExtendedMessageMap() {
        return new HashMap<>();
    }

    /**
     * Gets message map.
     *
     * @return the message map
     */
    @Bean("messageMap")
    public Map<String, String> getMessageMap() {
        return new HashMap<>();
    }


    /**
     * Feign form encoder encoder.
     *
     * @param converters the converters
     * @return the encoder
     */
    @Bean
    Encoder feignFormEncoder(ObjectFactory<HttpMessageConverters> converters) {
        return new SpringFormEncoder(new SpringEncoder(converters));
    }
}

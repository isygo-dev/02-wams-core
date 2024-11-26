package eu.isygoit.config;

import eu.isygoit.i18n.helper.LocaleResolver;
import lombok.extern.slf4j.Slf4j;
import org.jasypt.digest.StringDigester;
import org.jasypt.encryption.StringEncryptor;
import org.jasypt.util.password.PasswordEncryptor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * The type App config.
 */
@Slf4j
@Configuration
@EnableCaching //https://docs.spring.io/spring-framework/reference/integration/cache/annotations.html
@EnableConfigurationProperties(AppProperties.class)
public class AppConfig {

    /**
     * Local resolver locale resolver.
     *
     * @return the locale resolver
     */
    @Bean(name = "localResolver")
    public LocaleResolver localResolver() {
        return new LocaleResolver();
    }

    /**
     * Extended message map map.
     *
     * @return the map
     */
    @Bean("extendedMessageMap")
    public Map<String, String> extendedMessageMap() {
        return new HashMap<>();
    }

    /**
     * Message map map.
     *
     * @return the map
     */
    @Bean("messageMap")
    public Map<String, String> messageMap() {
        return new HashMap<>();
    }

    /**
     * Peb encriptor map map.
     *
     * @return the map
     */
    @Bean("pebEncriptorMap")
    public Map<String, StringEncryptor> pebEncriptorMap() {
        return new HashMap<>();
    }

    /**
     * Digest encriptor map map.
     *
     * @return the map
     */
    @Bean("digestEncriptorMap")
    public Map<String, StringDigester> digestEncriptorMap() {
        return new HashMap<>();
    }

    /**
     * Password encriptor map map.
     *
     * @return the map
     */
    @Bean("passwordEncriptorMap")
    public Map<String, PasswordEncryptor> passwordEncriptorMap() {
        return new HashMap<>();
    }
}

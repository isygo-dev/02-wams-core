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
import org.springframework.mail.MailSender;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * The type App config.
 */
@Configuration
@EnableCaching //https://docs.spring.io/spring-framework/reference/integration/cache/annotations.html
@EnableConfigurationProperties(AppProperties.class)
public class AppConfig {

    private final AppProperties appProperties;

    /**
     * Instantiates a new App config.
     *
     * @param appProperties the app properties
     */
    public AppConfig(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

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
     * Mail senders map.
     *
     * @return the map
     */
    @Bean
    public Map<String, MailSender> mailSenders() {
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
        //TODO try to add HashmapEncoder
        return new SpringFormEncoder(new SpringEncoder(converters));
    }

    /**
     * Default sender java mail sender.
     *
     * @return the java mail sender
     */
    @Bean(name = "defaultSender")
    public JavaMailSender defaultSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(appProperties.getMailHost());
        mailSender.setPort(appProperties.getMailPort());
        mailSender.setUsername(appProperties.getMailUserName());
        mailSender.setPassword(appProperties.getMailPassword());
        mailSender.setProtocol(appProperties.getMailProtocol());
        // Configure additional properties if needed
        Properties properties = new Properties();
        properties.put("mail.smtp.auth", appProperties.getMailSmtpAuth());
        properties.put("mail.smtp.starttls.enable", appProperties.getMailSmtpStarttls());
        mailSender.setJavaMailProperties(properties);

        return mailSender;
    }
}

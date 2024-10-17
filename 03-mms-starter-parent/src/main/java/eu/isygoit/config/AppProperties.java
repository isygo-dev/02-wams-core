package eu.isygoit.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * The type App properties.
 */
@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties extends ComAppProperties {

    @Value("${spring.mail.host}")
    private String mailHost;
    @Value("${spring.mail.port}")
    private Integer mailPort;
    @Value("${spring.mail.debug}")
    private String mailDebug;
    @Value("${spring.mail.username}")
    private String mailUserName;
    @Value("${spring.mail.password}")
    private String mailPassword;
    @Value("${spring.mail.properties.mail.smtp.auth}")
    private String mailSmtpAuth;
    @Value("${spring.mail.transport.protocol}")
    private String mailProtocol;
    @Value("${spring.mail.properties.mail.smtp.connectiontimeout}")
    private String mailSmtpConTimeout;
    @Value("${spring.mail.properties.mail.smtp.timeout}")
    private String mailSmtpTimeout;
    @Value("${spring.mail.properties.mail.smtp.writetimeout}")
    private String mailSmtpWriteTimeout;
    @Value("${spring.mail.properties.mail.smtp.starttls.enable}")
    private String mailSmtpStarttls;
}

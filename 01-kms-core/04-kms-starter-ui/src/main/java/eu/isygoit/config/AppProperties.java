package eu.isygoit.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The type App properties.
 */
@Data
@ConfigurationProperties(prefix = "app")
public class AppProperties extends ComAppProperties {

    @Value("${app.jwt.deactivateOldTokens}")
    private boolean deactivateOldTokens;

    @Value("${app.quartz.password.check-period-day}")
    private Integer pwdExpiredCheckPeriodDay;

    @Value("${app.quartz.password.less-remaining-days}")
    private Integer pwdExpiredLessRemainigDays;
}

package eu.isygoit.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The type App properties.
 * <p>
 * Registered as a bean through {@code @EnableConfigurationProperties} (AppConfig) / {@code @ConfigurationPropertiesScan}
 * exactly like the pilot (no additional {@code @Component}, which would create a second bean of the same type).
 */
@Data
@ConfigurationProperties(prefix = "app")
public class AppProperties extends ComAppProperties {

    // Same properties as the pilot, with defaults so the plan service starts without the identity-only keys.
    @Value("${app.jwt.deactivateOldTokens:false}")
    private boolean deactivateOldTokens;

    @Value("${app.quartz.password.check-period-day:1}")
    private Integer pwdExpiredCheckPeriodDay;

    @Value("${app.quartz.password.less-remaining-days:7}")
    private Integer pwdExpiredLessRemainigDays;
}

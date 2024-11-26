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

    @Value("${app.dms.local-storage}")
    private Boolean localStorageActive;
    @Value("${app.dms.no-duplicate}")
    private Boolean doNotDuplicate;
}

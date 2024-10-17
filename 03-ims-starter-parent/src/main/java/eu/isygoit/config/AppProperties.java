package eu.isygoit.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The type App properties.
 */
@Data
@ConfigurationProperties(prefix = "app")
public class AppProperties extends ComAppProperties {

}

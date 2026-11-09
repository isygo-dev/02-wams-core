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

}

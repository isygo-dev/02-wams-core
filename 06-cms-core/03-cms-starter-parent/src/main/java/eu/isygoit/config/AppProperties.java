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

    @Value("${app.calendar.ics.repository}")
    private String calanedarRepo;

    @Value("${app.calendar.create-if-not-exists}")
    private Boolean createCalendarIfNotExists;
}

package eu.isygoit.config;

import eu.isygoit.converter.StringToMapConverter;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final StringToMapConverter stringToMapConverter;

    public WebMvcConfig(StringToMapConverter stringToMapConverter) {
        this.stringToMapConverter = stringToMapConverter;
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(stringToMapConverter);
    }
}
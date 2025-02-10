package eu.isygoit;

import eu.isygoit.annotation.IgnoreRepository;
import eu.isygoit.api.IApiExtractor;
import eu.isygoit.app.ApplicationContextService;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * The type Kms starter.
 */
//http://localhost:40403/swagger-ui/index.html
@SpringBootApplication
@EnableFeignClients
@EnableConfigurationProperties
@ConfigurationPropertiesScan
@EntityScan(basePackages = {"eu.isygoit.model"})
@EnableJpaRepositories(basePackages = {"eu.isygoit.repository"}
        , excludeFilters = {@ComponentScan.Filter(IgnoreRepository.class)})
@OpenAPIDefinition(info =
@Info(title = "Key management API", version = "1.0", description = "Documentation Key management API v1.0")
)
@PropertySource(encoding = "UTF-8", value = {"classpath:i18n/messages.properties"})
public class KmsStarter extends ServiceStarter {

    private final ApplicationContextService applicationContextService;
    private final IApiExtractor apiExtractor;

    @Autowired
    public KmsStarter(ApplicationContextService applicationContextService, IApiExtractor apiExtractor) {
        this.applicationContextService = applicationContextService;
        this.apiExtractor = apiExtractor;
    }

    /**
     * The entry point of application.
     *
     * @param args the input arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(KmsStarter.class, args);
    }

    @Override
    protected ApplicationContextService getApplicationContextServiceInstance() {
        return applicationContextService;
    }

    @Override
    protected IApiExtractor getApiExtractorInstance() {
        return apiExtractor;
    }
}

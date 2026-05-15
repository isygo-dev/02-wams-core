package eu.isygoit;


import eu.isygoit.api.IApiExtractor;
import eu.isygoit.app.ApplicationContextService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.PropertySource;

/**
 * The type Kms starter.
 */
//http://localhost:80403/swagger-ui/index.html
@SpringBootApplication
@EnableFeignClients
@EnableConfigurationProperties
@ConfigurationPropertiesScan
@EntityScan(basePackages = {"eu.isygoit.model"})
@PropertySource(encoding = "UTF-8", value = {"classpath:i18n/messages.properties"})
public class KmsUiStarter {

    protected KmsUiStarter() {
        super();
    }

    /**
     * The entry point of application.
     *
     * @param args the input arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(KmsUiStarter.class, args);
    }
}

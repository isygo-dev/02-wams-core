package eu.isygoit.config;


import io.minio.MinioClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;


/**
 * The type Minio configuration.
 */
@Configuration
public class MinioConfiguration {

    /**
     * Min io map map.
     *
     * @return the map
     */
    @Bean
    public Map<String, MinioClient> minIoMap() {
        return new HashMap<>();
    }
}

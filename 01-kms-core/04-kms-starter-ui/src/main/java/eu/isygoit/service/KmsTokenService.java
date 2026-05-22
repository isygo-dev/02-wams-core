package eu.isygoit.service;


import eu.isygoit.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * The interface Kms token service.
 */
@FeignClient(configuration = FeignConfig.class, name = "key-service", contextId = "kms-token", path = "/api/v1/private/token")
public interface KmsTokenService extends TokenServiceApi {

}

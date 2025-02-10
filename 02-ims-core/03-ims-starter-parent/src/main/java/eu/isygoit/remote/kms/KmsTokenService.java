package eu.isygoit.remote.kms;


import eu.isygoit.config.FeignConfig;
import eu.isygoit.service.token.TokenServiceApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * The interface Kms token service.
 */
@FeignClient(configuration = FeignConfig.class, name = "key-service", contextId = "kms-token", path = "/api/v1/private/token")
public interface KmsTokenService extends TokenServiceApi {

}

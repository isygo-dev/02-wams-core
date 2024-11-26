package eu.isygoit.remote.kms;

import eu.isygoit.api.PublicPasswordControllerApi;
import eu.isygoit.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * The interface Kms public password service.
 */
@FeignClient(configuration = FeignConfig.class, name = "key-service", contextId = "kms-public-password", path = "/api/v1/public/password")
public interface KmsPublicPasswordService extends PublicPasswordControllerApi {

}

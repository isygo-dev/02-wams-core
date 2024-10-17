package eu.isygoit.remote.kms;

import eu.isygoit.api.PasswordControllerApi;
import eu.isygoit.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * The interface Kms password service.
 */
@FeignClient(configuration = FeignConfig.class, name = "key-service", contextId = "kms-password", path = "/api/v1/private/password")
public interface KmsPasswordService extends PasswordControllerApi {

}

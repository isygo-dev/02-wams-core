package eu.isygoit.remote.kms;

import eu.isygoit.api.IncrementalKeyControllerApi;
import eu.isygoit.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * The interface Kms incremental key service.
 */
@FeignClient(configuration = FeignConfig.class, name = "key-service", contextId = "incremental-key", path = "/api/v1/private/key")
public interface KmsIncrementalKeyService extends IncrementalKeyControllerApi {

}

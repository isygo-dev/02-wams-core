package eu.isygoit.remote.ims;

import eu.isygoit.api.PublicServiceApi;
import eu.isygoit.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * The interface Ims public service.
 */
@FeignClient(configuration = FeignConfig.class, name = "identity-service", contextId = "ims-public", path = "/api/v1/public")
public interface ImsPublicService extends PublicServiceApi {

}

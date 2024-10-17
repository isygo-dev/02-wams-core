package eu.isygoit.remote.ims;

import eu.isygoit.api.AppParameterControllerApi;
import eu.isygoit.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * The interface Ims app parameter service.
 */
@FeignClient(configuration = FeignConfig.class, name = "identity-service", contextId = "ims-app-parameter", path = "/api/v1/private/appParameter")
public interface ImsAppParameterService extends AppParameterControllerApi {

}

package eu.isygoit.remote.ims;

import eu.isygoit.api.AppParameterServiceApi;
import eu.isygoit.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(configuration = FeignConfig.class, name = "identity-service", contextId = "app-parameter", path = "/api/v1/private/appParameter")
public interface AppParameterService extends AppParameterServiceApi {

}
package eu.isygoit.remote.ims;

import eu.isygoit.api.ApplicationServiceApi;
import eu.isygoit.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(configuration = FeignConfig.class, name = "identity-service", contextId = "application", path = "/api/v1/private/application")
public interface ApplicationService extends ApplicationServiceApi {

}
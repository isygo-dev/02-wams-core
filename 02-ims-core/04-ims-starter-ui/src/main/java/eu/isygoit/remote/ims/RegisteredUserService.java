package eu.isygoit.remote.ims;

import eu.isygoit.api.AccountServiceApi;
import eu.isygoit.api.RegisteredUserServiceApi;
import eu.isygoit.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(configuration = FeignConfig.class, name = "identity-service", contextId = "registered-user", path = "/api/v1/private/registred-user")
public interface RegisteredUserService extends RegisteredUserServiceApi {

}
package eu.isygoit.remote.ims;

import eu.isygoit.api.AccountServiceApi;
import eu.isygoit.api.ProfileServiceApi;
import eu.isygoit.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(configuration = FeignConfig.class, name = "identity-service", contextId = "profile", path = "/api/v1/private/profile")
public interface ProfileService extends ProfileServiceApi {

}
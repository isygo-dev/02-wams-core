package eu.isygoit.remote.ims;

import eu.isygoit.api.AccountServiceApi;
import eu.isygoit.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(configuration = FeignConfig.class, name = "identity-service", contextId = "account", path = "/api/v1/private/account")
public interface AccountService extends AccountServiceApi {

}
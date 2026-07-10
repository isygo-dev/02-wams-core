package eu.isygoit.remote.ims;

import eu.isygoit.api.CustomerServiceApi;
import eu.isygoit.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(configuration = FeignConfig.class, name = "identity-service", contextId = "customer", path = "/api/v1/private/customer")
public interface CustomerService extends CustomerServiceApi {

}
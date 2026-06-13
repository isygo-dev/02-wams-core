package eu.isygoit.remote.ims;

import eu.isygoit.api.TenantServiceApi;
import eu.isygoit.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(configuration = FeignConfig.class, name = "identity-service", contextId = "tenant", path = "/api/v1/private/tenant")
public interface TenantService extends TenantServiceApi {

}
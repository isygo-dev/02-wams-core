package eu.isygoit.remote.ims;

import eu.isygoit.api.TenantControllerApi;
import eu.isygoit.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * The interface Ims tenant service.
 */
@FeignClient(configuration = FeignConfig.class, name = "identity-service", contextId = "ims-tenant", path = "/api/v1/private/tenant")
public interface ImsTenantService extends TenantControllerApi {

}

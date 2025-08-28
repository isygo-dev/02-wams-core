package eu.isygoit.remote.kms;

import eu.isygoit.api.KmsTenantControllerApi;
import eu.isygoit.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * The interface Kms tenant service.
 */
@FeignClient(configuration = FeignConfig.class, name = "key-service", contextId = "kms-tenant", path = "/api/v1/private/tenant")
public interface KmsTenantService extends KmsTenantControllerApi {

}

package eu.isygoit.remote.ims;

import eu.isygoit.api.DomainControllerApi;
import eu.isygoit.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * The interface Ims domain service.
 */
@FeignClient(configuration = FeignConfig.class, name = "identity-service", contextId = "ims-domain", path = "/api/v1/private/domain")
public interface ImsDomainService extends DomainControllerApi {

}

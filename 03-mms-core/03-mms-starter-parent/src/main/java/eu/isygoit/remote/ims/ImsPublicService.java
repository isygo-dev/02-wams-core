package eu.isygoit.remote.ims;

import eu.isygoit.api.PublicAuthServiceApi;
import eu.isygoit.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * The interface Ims public service.
 */
@FeignClient(configuration = FeignConfig.class, name = "identity-service", contextId = "public-auth", path = "/api/v1/public/auth")
public interface ImsPublicService extends PublicAuthServiceApi {

}

package eu.isygoit.remote.ims;

import eu.isygoit.api.PublicAuthServiceApi;
import eu.isygoit.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * The interface Public auth controller api.
 */
@FeignClient(configuration = FeignConfig.class, name = "identity-service", contextId = "public-auth", path = "/api/v1/public/auth")
public interface PublicAuthService extends PublicAuthServiceApi {

}

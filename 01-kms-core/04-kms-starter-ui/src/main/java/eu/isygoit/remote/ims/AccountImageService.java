package eu.isygoit.remote.ims;

import eu.isygoit.api.PublicAccountImageServiceApi;
import eu.isygoit.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(configuration = FeignConfig.class, name = "identity-service", contextId = "account-image", path = "/api/v1/private/account")
public interface AccountImageService extends PublicAccountImageServiceApi {

}
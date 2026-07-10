package eu.isygoit.remote.ims;

import eu.isygoit.api.AnnexServiceApi;
import eu.isygoit.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(configuration = FeignConfig.class, name = "identity-service", contextId = "annex", path = "/api/v1/private/annex")
public interface AnnexService extends AnnexServiceApi {

}
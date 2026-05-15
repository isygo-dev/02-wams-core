package eu.isygoit.remote.kms;

import eu.isygoit.api.KmsServiceApi;
import eu.isygoit.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(configuration = FeignConfig.class, name = "key-service", contextId = "kms-api", path = "/api/v1/private/kms")
public interface KmsApiService extends KmsServiceApi {

}

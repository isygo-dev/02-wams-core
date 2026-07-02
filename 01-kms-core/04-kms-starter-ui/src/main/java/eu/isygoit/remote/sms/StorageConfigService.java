package eu.isygoit.remote.sms;

import eu.isygoit.api.StorageConfigServiceApi;
import eu.isygoit.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(configuration = FeignConfig.class, name = "storage-service", contextId = "storage-config", path = "/api/v1/private/storage/config")
public interface StorageConfigService extends StorageConfigServiceApi {

}
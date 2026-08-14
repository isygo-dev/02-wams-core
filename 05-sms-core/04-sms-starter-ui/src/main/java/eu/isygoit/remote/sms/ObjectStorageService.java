package eu.isygoit.remote.sms;

import eu.isygoit.api.ObjectStorageServiceApi;
import eu.isygoit.api.StorageConfigServiceApi;
import eu.isygoit.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(configuration = FeignConfig.class, name = "storage-service", contextId = "storage-object", path = "/api/v1/private/storage")
public interface ObjectStorageService extends ObjectStorageServiceApi {

}
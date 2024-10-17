package eu.isygoit.remote.sms;

import eu.isygoit.api.ObjectStorageControllerApi;
import eu.isygoit.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * The interface Sms storage linked file service.
 */
@FeignClient(configuration = FeignConfig.class, name = "storage-service", contextId = "sms-storage", path = "/api/v1/private/storage")
public interface SmsStorageLinkedFileService extends ObjectStorageControllerApi {
}

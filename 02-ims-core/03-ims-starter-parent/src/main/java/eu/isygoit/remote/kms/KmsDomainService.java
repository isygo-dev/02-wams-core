package eu.isygoit.remote.kms;

import eu.isygoit.api.KmsDomainControllerApi;
import eu.isygoit.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * The interface Kms domain service.
 */
@FeignClient(configuration = FeignConfig.class, name = "key-service", contextId = "kms-domain", path = "/api/v1/private/domain")
public interface KmsDomainService extends KmsDomainControllerApi {

}

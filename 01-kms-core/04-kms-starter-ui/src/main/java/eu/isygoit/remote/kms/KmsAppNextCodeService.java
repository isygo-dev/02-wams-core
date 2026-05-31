package eu.isygoit.remote.kms;

import eu.isygoit.api.IncrementalKeyServiceApi;
import eu.isygoit.com.rest.api.IMappedCrudApi;
import eu.isygoit.config.FeignConfig;
import eu.isygoit.dto.common.NextCodeDto;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * The interface Kms incremental key service.
 */
@FeignClient(configuration = FeignConfig.class, name = "key-service", contextId = "next-code", path = "/api/v1/private/code")
public interface KmsAppNextCodeService extends IMappedCrudApi<Long, NextCodeDto, NextCodeDto>, IncrementalKeyServiceApi {

}

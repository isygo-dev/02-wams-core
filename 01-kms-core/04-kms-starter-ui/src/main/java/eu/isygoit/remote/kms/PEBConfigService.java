package eu.isygoit.remote.kms;

import eu.isygoit.com.rest.api.IMappedCrudApi;
import eu.isygoit.config.FeignConfig;
import eu.isygoit.dto.data.PEBConfigDto;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * The interface Kms incremental key service.
 */
@FeignClient(configuration = FeignConfig.class, name = "key-service", contextId = "peb-config", path = "/api/v1/private/config/peb")
public interface PEBConfigService extends IMappedCrudApi<Long, PEBConfigDto, PEBConfigDto> {

}

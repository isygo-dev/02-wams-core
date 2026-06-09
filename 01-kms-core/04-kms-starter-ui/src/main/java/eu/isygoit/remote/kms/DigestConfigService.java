package eu.isygoit.remote.kms;

import eu.isygoit.com.rest.api.IMappedCrudApi;
import eu.isygoit.config.FeignConfig;
import eu.isygoit.dto.data.DigestConfigDto;
import eu.isygoit.dto.data.TokenConfigDto;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * The interface Kms incremental key service.
 */
@FeignClient(configuration = FeignConfig.class, name = "key-service", contextId = "digest-config", path = "/api/v1/private/config/digest")
public interface DigestConfigService extends IMappedCrudApi<Long, DigestConfigDto, DigestConfigDto> {

}

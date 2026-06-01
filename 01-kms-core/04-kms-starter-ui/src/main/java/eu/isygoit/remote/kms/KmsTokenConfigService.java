package eu.isygoit.remote.kms;

import eu.isygoit.com.rest.api.IMappedCrudApi;
import eu.isygoit.config.FeignConfig;
import eu.isygoit.dto.common.NextCodeDto;
import eu.isygoit.dto.data.TokenConfigDto;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * The interface Kms incremental key service.
 */
@FeignClient(configuration = FeignConfig.class, name = "key-service", contextId = "token-config", path = "/api/v1/private/config/token")
public interface KmsTokenConfigService extends IMappedCrudApi<Long, TokenConfigDto, TokenConfigDto> {

}

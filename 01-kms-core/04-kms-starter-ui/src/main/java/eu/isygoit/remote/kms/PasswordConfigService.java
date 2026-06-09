package eu.isygoit.remote.kms;

import eu.isygoit.com.rest.api.IMappedCrudApi;
import eu.isygoit.config.FeignConfig;
import eu.isygoit.dto.data.PasswordConfigDto;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * The interface Kms incremental key service.
 */
@FeignClient(configuration = FeignConfig.class, name = "key-service", contextId = "password-config", path = "/api/v1/private/config/password")
public interface PasswordConfigService extends IMappedCrudApi<Long, PasswordConfigDto, PasswordConfigDto> {

}

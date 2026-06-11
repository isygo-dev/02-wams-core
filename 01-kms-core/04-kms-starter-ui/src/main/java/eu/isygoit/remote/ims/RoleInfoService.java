package eu.isygoit.remote.ims;

import eu.isygoit.com.rest.api.IMappedCrudApi;
import eu.isygoit.config.FeignConfig;
import eu.isygoit.dto.data.RoleInfoDto;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(configuration = FeignConfig.class, name = "identity-service", contextId = "role-info", path = "/api/v1/private/roleInfo")
public interface RoleInfoService extends IMappedCrudApi<Long, RoleInfoDto, RoleInfoDto> {
}
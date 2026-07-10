package eu.isygoit.remote.ims;

import eu.isygoit.com.rest.api.IMappedCrudApi;
import eu.isygoit.config.FeignConfig;
import eu.isygoit.dto.data.AccountDetailsDto;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(configuration = FeignConfig.class, name = "identity-service", contextId = "account-details", path = "/api/v1/private/account/details")
public interface AccountDetailsService extends IMappedCrudApi<Long, AccountDetailsDto, AccountDetailsDto> {
}
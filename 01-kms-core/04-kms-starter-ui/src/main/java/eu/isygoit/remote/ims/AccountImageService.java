package eu.isygoit.remote.ims;

import eu.isygoit.com.rest.api.IMappedCrudApi;
import eu.isygoit.com.rest.api.IMappedImageApi;
import eu.isygoit.config.FeignConfig;
import eu.isygoit.dto.data.AccountDto;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(configuration = FeignConfig.class, name = "identity-service", contextId = "account-image", path = "/api/v1/private/account")
public interface AccountImageService extends IMappedImageApi<Long, AccountDto> {

}
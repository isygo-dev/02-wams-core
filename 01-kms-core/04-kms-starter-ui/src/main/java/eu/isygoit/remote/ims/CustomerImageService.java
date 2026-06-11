package eu.isygoit.remote.ims;

import eu.isygoit.com.rest.api.IMappedCrudApi;
import eu.isygoit.config.FeignConfig;
import eu.isygoit.dto.data.CustomerDto;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(configuration = FeignConfig.class, name = "identity-service", contextId = "customer-image", path = "/api/v1/private/customer")
public interface CustomerImageService extends IMappedCrudApi<Long, CustomerDto, CustomerDto> {
}
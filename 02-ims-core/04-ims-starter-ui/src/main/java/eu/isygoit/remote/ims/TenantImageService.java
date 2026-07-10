package eu.isygoit.remote.ims;

import eu.isygoit.com.rest.api.IMappedImageApi;
import eu.isygoit.config.FeignConfig;
import eu.isygoit.dto.data.TenantDto;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(configuration = FeignConfig.class, name = "identity-service", contextId = "tenant-image", path = "/api/v1/private/tenant")
public interface TenantImageService extends IMappedImageApi<Long, TenantDto> {
    // No extra endpoints
}
package eu.isygoit.remote.ims;

import eu.isygoit.com.rest.api.IMappedImageApi;
import eu.isygoit.config.FeignConfig;
import eu.isygoit.dto.data.ApplicationDto;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(configuration = FeignConfig.class, name = "identity-service", contextId = "application-image", path = "/api/v1/private/application")
public interface AppImageService extends IMappedImageApi<Long, ApplicationDto> {
}
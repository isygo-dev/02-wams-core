package eu.isygoit.remote.ims;

import eu.isygoit.com.rest.api.IMappedCrudApi;
import eu.isygoit.config.FeignConfig;
import eu.isygoit.dto.common.ContextRequestDto;
import eu.isygoit.dto.common.PaginatedResponseDto;
import eu.isygoit.dto.data.ApplicationDto;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(configuration = FeignConfig.class, name = "identity-service", contextId = "application", path = "/api/v1/private/application")
public interface AppService extends IMappedCrudApi<Long, ApplicationDto, ApplicationDto> {

    @PutMapping("/update-status")
    ApplicationDto updateStatus(@RequestParam("id") Long id,
                                @RequestParam("newStatus") IEnumEnabledBinaryStatus.Types newStatus);

    // Overrides of performFindAll / performFindAllFull – these are mapped to "/find-all" and "/find-all-full" in the superclass.
    // If needed, declare them explicitly:
    @GetMapping("/find-all")
    PaginatedResponseDto<ApplicationDto> performFindAll(@RequestBody ContextRequestDto requestContext,
                                                        @RequestParam(value = "page", required = false) Integer page,
                                                        @RequestParam(value = "size", required = false) Integer size);

    @GetMapping("/find-all-full")
    PaginatedResponseDto<ApplicationDto> performFindAllFull(@RequestBody ContextRequestDto requestContext,
                                                            @RequestParam(value = "page", required = false) Integer page,
                                                            @RequestParam(value = "size", required = false) Integer size);
}
package eu.isygoit.controller;

import eu.isygoit.annotation.InjectMapperAndService;
import eu.isygoit.com.rest.controller.ResponseFactory;
import eu.isygoit.com.rest.controller.impl.tenancy.MappedCrudTenantController;
import eu.isygoit.constants.JwtConstants;
import eu.isygoit.constants.RestApiConstants;
import eu.isygoit.dto.common.ContextRequestDto;
import eu.isygoit.dto.data.StorageConfigDto;
import eu.isygoit.dto.extendable.IdAssignableDto;
import eu.isygoit.exception.handler.SmsExceptionHandler;
import eu.isygoit.mapper.StorageConfigMapper;
import eu.isygoit.model.StorageConfig;
import eu.isygoit.service.impl.StorageConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * The type Storage config controller.
 */
@Slf4j
@Validated
@RestController
@RequestMapping(path = "/api/v1/private/storage/config")
@InjectMapperAndService(handler = SmsExceptionHandler.class, mapper = StorageConfigMapper.class, minMapper = StorageConfigMapper.class, service = StorageConfigService.class)
public class StorageConfigController extends MappedCrudTenantController<Long, StorageConfig, StorageConfigDto, StorageConfigDto, StorageConfigService> {


    /**
     * Find by tenant ignore case response entity.
     *
     * @param requestContext the request context
     * @param tenant         the tenant
     * @return the response entity
     */
    @Operation(summary = "findByTenantIgnoreCase Api",
            description = "findByTenantIgnoreCase")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = IdAssignableDto.class))})
    })
    @GetMapping(path = "/tenant/{tenant}")
    public ResponseEntity<StorageConfigDto> findByTenantIgnoreCase(@RequestAttribute(value = JwtConstants.JWT_USER_CONTEXT) ContextRequestDto requestContext,
                                                                   @PathVariable(name = RestApiConstants.TENANT_NAME) String tenant) {
        try {
            return ResponseFactory.responseOk(this.mapper().entityToDto(crudService().findByTenantIgnoreCase(tenant)));
        } catch (Throwable e) {
            log.error("<Error>: Error calling api getNotificationsByReceiverId : {}", e);
            return getBackExceptionResponse(e);
        }
    }
}

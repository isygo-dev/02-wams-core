package eu.isygoit.controller;

import eu.isygoit.annotation.CtrlDef;
import eu.isygoit.com.rest.controller.ResponseFactory;
import eu.isygoit.com.rest.controller.impl.MappedCrudController;
import eu.isygoit.constants.JwtConstants;
import eu.isygoit.constants.RestApiConstants;
import eu.isygoit.dto.common.RequestContextDto;
import eu.isygoit.dto.data.StorageConfigDto;
import eu.isygoit.dto.extendable.IdentifiableDto;
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
@CtrlDef(handler = SmsExceptionHandler.class, mapper = StorageConfigMapper.class, minMapper = StorageConfigMapper.class, service = StorageConfigService.class)
public class StorageConfigController extends MappedCrudController<Long, StorageConfig, StorageConfigDto, StorageConfigDto, StorageConfigService> {


    /**
     * Find by domain ignore case response entity.
     *
     * @param requestContext the request context
     * @param domain         the domain
     * @return the response entity
     */
    @Operation(summary = "findByDomainIgnoreCase Api",
            description = "findByDomainIgnoreCase")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = IdentifiableDto.class))})
    })
    @GetMapping(path = "/domain/{domain}")
    public ResponseEntity<StorageConfigDto> findByDomainIgnoreCase(@RequestAttribute(value = JwtConstants.JWT_USER_CONTEXT) RequestContextDto requestContext,
                                                                   @PathVariable(name = RestApiConstants.DOMAIN_NAME) String domain) {
        try {
            return ResponseFactory.ResponseOk(this.mapper().entityToDto(crudService().findByDomainIgnoreCase(domain)));
        } catch (Throwable e) {
            log.error("<Error>: Error calling api getNotificationsByReceiverId : {}", e);
            return getBackExceptionResponse(e);
        }
    }
}

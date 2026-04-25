package eu.isygoit.controller;

import eu.isygoit.annotation.InjectMapperAndService;
import eu.isygoit.api.KmsTenantControllerApi;
import eu.isygoit.com.rest.controller.ResponseFactory;
import eu.isygoit.com.rest.controller.constants.CtrlConstants;
import eu.isygoit.com.rest.controller.impl.MappedCrudController;
import eu.isygoit.dto.data.KmsTenantDto;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.exception.handler.KmsExceptionHandler;
import eu.isygoit.mapper.TenantMapper;
import eu.isygoit.model.Tenant;
import eu.isygoit.service.impl.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The type Kms tenant controller.
 */
@Slf4j
@Validated
@Tag(name = "KMS Tenant", description = "Endpoints for managing KMS tenants")
@RestController
@InjectMapperAndService(handler = KmsExceptionHandler.class, mapper = TenantMapper.class, minMapper = TenantMapper.class, service = TenantService.class)
@RequestMapping(path = "/api/v1/private/tenant")
public class KmsTenantController extends MappedCrudController<Long, Tenant, KmsTenantDto, KmsTenantDto, TenantService>
        implements KmsTenantControllerApi {

    @Operation(summary = "Update admin status Api",
            description = "Update admin status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = KmsTenantDto.class))})
    })
    @Override
    public ResponseEntity<KmsTenantDto> updateAdminStatus(
            String tenant,
            IEnumEnabledBinaryStatus.Types newStatus) {
        log.info("in update status");
        try {
            return ResponseFactory.responseOk(mapper().entityToDto(crudService().updateAdminStatus(tenant, newStatus)));
        } catch (Throwable e) {
            log.error("<Error>: update Domain Status : {} ", e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<Boolean> updateDomain(
            KmsTenantDto tenant) {
        log.info("Call update tenant " + tenant.toString());
        try {
            return ResponseFactory.responseOk(crudService().checkIfExists(mapper().dtoToEntity(tenant),
                    true));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }
}

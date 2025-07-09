package eu.isygoit.controller;

import eu.isygoit.annotation.InjectMapperAndService;
import eu.isygoit.com.rest.controller.ResponseFactory;
import eu.isygoit.com.rest.controller.constants.CtrlConstants;
import eu.isygoit.com.rest.controller.impl.tenancy.MappedCrudTenantController;
import eu.isygoit.constants.JwtConstants;
import eu.isygoit.constants.RestApiConstants;
import eu.isygoit.constants.TenantConstants;
import eu.isygoit.dto.common.RequestContextDto;
import eu.isygoit.dto.data.ApplicationDto;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.exception.handler.ImsExceptionHandler;
import eu.isygoit.mapper.ApplicationMapper;
import eu.isygoit.model.Application;
import eu.isygoit.service.IAccountService;
import eu.isygoit.service.impl.ApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * The type App controller.
 */
@Slf4j
@Validated
@RestController
@InjectMapperAndService(handler = ImsExceptionHandler.class, mapper = ApplicationMapper.class, minMapper = ApplicationMapper.class, service = ApplicationService.class)
@RequestMapping(path = "/api/v1/private/application")
public class AppController extends MappedCrudTenantController<Long, Application, ApplicationDto, ApplicationDto, ApplicationService> {

    @Autowired
    private IAccountService accountService;

    /**
     * Update status response entity.
     *
     * @param requestContext the request context
     * @param id             the id
     * @param newStatus      the new status
     * @return the response entity
     */
    @Operation(summary = "Update application status Api",
            description = "Update application status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApplicationDto.class))})
    })
    @PutMapping(path = "/update-status")
    public ResponseEntity<ApplicationDto> updateStatus(@RequestAttribute(value = JwtConstants.JWT_USER_CONTEXT) RequestContextDto requestContext,
                                                       @RequestParam(name = RestApiConstants.ID) Long id,
                                                       @RequestParam(name = RestApiConstants.NEW_STATUS) IEnumEnabledBinaryStatus.Types newStatus) {
        log.info("in update status");
        try {
            return ResponseFactory.responseOk(mapper().entityToDto(crudService().updateStatus(id, newStatus)));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<List<ApplicationDto>> subFindAll(RequestContextDto requestContext) {
        if (TenantConstants.SUPER_TENANT_NAME.equals(requestContext.getSenderTenant())) {
            return super.subFindAll(requestContext);
        } else {
            return ResponseFactory.responseOk(mapper().listEntityToDto(accountService.findDistinctAllowedToolsByTenantAndUserName(requestContext.getSenderTenant(),
                    requestContext.getSenderUser())));
        }
    }

    @Override
    public ResponseEntity<List<ApplicationDto>> subFindAllDefault(RequestContextDto requestContext) {
        if (TenantConstants.SUPER_TENANT_NAME.equals(requestContext.getSenderTenant())) {
            return super.subFindAllDefault(requestContext);
        } else {
            return ResponseFactory.responseOk(mapper().listEntityToDto(accountService.findDistinctAllowedToolsByTenantAndUserName(requestContext.getSenderTenant(),
                    requestContext.getSenderUser())));
        }
    }

    @Override
    public ResponseEntity<List<ApplicationDto>> subFindAll(RequestContextDto requestContext, Integer page, Integer size) {
        if (TenantConstants.SUPER_TENANT_NAME.equals(requestContext.getSenderTenant())) {
            return super.subFindAll(requestContext, page, size);
        } else {
            return ResponseFactory.responseOk(mapper().listEntityToDto(accountService.findDistinctAllowedToolsByTenantAndUserName(requestContext.getSenderTenant(),
                    requestContext.getSenderUser())));
        }
    }

    @Override
    public ResponseEntity<List<ApplicationDto>> subFindAllFull(RequestContextDto requestContext) {
        if (TenantConstants.SUPER_TENANT_NAME.equals(requestContext.getSenderTenant())) {
            return super.subFindAllFull(requestContext);
        } else {
            return ResponseFactory.responseOk(mapper().listEntityToDto(accountService.findDistinctAllowedToolsByTenantAndUserName(requestContext.getSenderTenant(),
                    requestContext.getSenderUser())));
        }
    }

    @Override
    public ResponseEntity<List<ApplicationDto>> subFindAllFull(RequestContextDto requestContext, Integer page, Integer size) {
        if (TenantConstants.SUPER_TENANT_NAME.equals(requestContext.getSenderTenant())) {
            return super.subFindAllFull(requestContext, page, size);
        } else {
            return ResponseFactory.responseOk(mapper().listEntityToDto(accountService.findDistinctAllowedToolsByTenantAndUserName(requestContext.getSenderTenant(),
                    requestContext.getSenderUser())));
        }
    }
}

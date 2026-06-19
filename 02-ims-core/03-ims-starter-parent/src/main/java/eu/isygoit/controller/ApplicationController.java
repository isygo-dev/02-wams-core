package eu.isygoit.controller;

import eu.isygoit.annotation.InjectMapperAndService;
import eu.isygoit.api.ApplicationServiceApi;
import eu.isygoit.com.rest.controller.ResponseFactory;
import eu.isygoit.com.rest.controller.constants.CtrlConstants;
import eu.isygoit.com.rest.controller.impl.tenancy.MappedCrudTenantController;
import eu.isygoit.constants.RestApiConstants;
import eu.isygoit.constants.TenantConstants;
import eu.isygoit.dto.common.RequestContextDto;
import eu.isygoit.dto.common.PaginatedResponseDto;
import eu.isygoit.dto.data.ApplicationDto;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.exception.handler.ImsExceptionHandler;
import eu.isygoit.mapper.ApplicationMapper;
import eu.isygoit.model.Application;
import eu.isygoit.service.IAccountService;
import eu.isygoit.service.impl.ApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The type App controller.
 */
@Slf4j
@Validated
@RestController
@InjectMapperAndService(handler = ImsExceptionHandler.class, mapper = ApplicationMapper.class, minMapper = ApplicationMapper.class, service = ApplicationService.class)
@RequestMapping(path = "/api/v1/private/application")
public class ApplicationController extends MappedCrudTenantController<Long, Application, ApplicationDto, ApplicationDto, ApplicationService>
        implements ApplicationServiceApi {

    @Autowired
    private IAccountService accountService;

    @Override
    public ResponseEntity<ApplicationDto> updateStatus(
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
    public ResponseEntity<PaginatedResponseDto<ApplicationDto>> performFindAll(RequestContextDto requestContext, Integer page, Integer size) {
        if (TenantConstants.SUPER_TENANT_NAME.equals(requestContext.getSenderTenant())) {
            return super.performFindAll(requestContext, page, size);
        } else {
            // For regular tenants: return distinct allowed tools (non-paginated)
            List<Application> applications = accountService.findDistinctAllowedToolsByTenantAndUserName(
                    requestContext.getSenderTenant(),
                    requestContext.getSenderUser());

            // Convert List to PaginatedResponseDto (single page with all results)
            PaginatedResponseDto<ApplicationDto> paginatedResponse = PaginatedResponseDto.<ApplicationDto>builder()
                    .content(mapper().listEntityToDto(applications))
                    .totalElements(applications != null ? (long) applications.size() : 0L)
                    .totalPages(1)
                    .pageNumber(0)
                    .pageSize(applications != null ? applications.size() : 0)
                    .build();

            return ResponseFactory.responseOk(paginatedResponse);
        }
    }

    @Override
    public ResponseEntity<PaginatedResponseDto<ApplicationDto>> performFindAllFull(RequestContextDto requestContext, Integer page, Integer size) {
        if (TenantConstants.SUPER_TENANT_NAME.equals(requestContext.getSenderTenant())) {
            return super.performFindAllFull(requestContext, page, size);
        } else {
            // Non-super tenant: return distinct allowed tools (not paginated)
            List<Application> applications = accountService.findDistinctAllowedToolsByTenantAndUserName(
                    requestContext.getSenderTenant(),
                    requestContext.getSenderUser());

            // Wrap the list into PaginatedResponseDto (with pagination metadata as empty/single page)
            PaginatedResponseDto<ApplicationDto> paginatedResponse = PaginatedResponseDto.<ApplicationDto>builder()
                    .content(mapper().listEntityToDto(applications))
                    .totalElements(applications != null ? (long) applications.size() : 0L)
                    .totalPages(1)
                    .pageNumber(0)
                    .pageSize(applications != null ? applications.size() : 0)
                    .build();

            return ResponseFactory.responseOk(paginatedResponse);
        }
    }
}

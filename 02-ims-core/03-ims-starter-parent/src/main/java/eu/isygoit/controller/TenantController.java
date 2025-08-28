package eu.isygoit.controller;

import eu.isygoit.annotation.InjectMapperAndService;
import eu.isygoit.api.TenantControllerApi;
import eu.isygoit.com.rest.controller.ResponseFactory;
import eu.isygoit.com.rest.controller.constants.CtrlConstants;
import eu.isygoit.com.rest.controller.impl.tenancy.MappedCrudTenantController;
import eu.isygoit.dto.common.ContextRequestDto;
import eu.isygoit.dto.data.KmsTenantDto;
import eu.isygoit.dto.data.TenantDto;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.exception.KmsTenantUpdateException;
import eu.isygoit.exception.handler.ImsExceptionHandler;
import eu.isygoit.mapper.TenantMapper;
import eu.isygoit.model.Tenant;
import eu.isygoit.remote.kms.KmsTenantService;
import eu.isygoit.service.impl.TenantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The type Domain controller.
 */
@Slf4j
@Validated
@RestController
@InjectMapperAndService(handler = ImsExceptionHandler.class, mapper = TenantMapper.class, minMapper = TenantMapper.class, service = TenantService.class)
@RequestMapping(path = "/api/v1/private/tenant")
public class TenantController extends MappedCrudTenantController<Long, Tenant, TenantDto, TenantDto, TenantService>
        implements TenantControllerApi {

    @Autowired
    private KmsTenantService kmsDomainService;

    @Override
    public Tenant afterUpdate(Tenant tenant) {
        try {
            ResponseEntity<Boolean> result = kmsDomainService.updateDomain(ContextRequestDto.builder().build(),
                    KmsTenantDto.builder()
                            .name(tenant.getName())
                            .description(tenant.getDescription())
                            .url(tenant.getUrl())
                            .adminStatus(tenant.getAdminStatus())
                            .build());
            if (result.getStatusCode().is2xxSuccessful() && result.hasBody() && result.getBody()) {
                return super.afterUpdate(tenant);
            }
        } catch (Exception e) {
            log.error("Remote feign call failed : ", e);
            //throw new RemoteCallFailedException(e);
        }

        throw new KmsTenantUpdateException("for tenant id: " + tenant.getId());
    }

    @Override
    public ResponseEntity<TenantDto> updateAdminStatus(ContextRequestDto requestContext,
                                                       Long id,
                                                       IEnumEnabledBinaryStatus.Types newStatus) {
        log.info("in update status");
        try {
            TenantDto tenant = mapper().entityToDto(crudService().updateAdminStatus(id, newStatus));
            try {
                ResponseEntity<KmsTenantDto> result = kmsDomainService.updateAdminStatus(ContextRequestDto.builder().build(),
                        tenant.getName(), newStatus);
                if (result.getStatusCode().is2xxSuccessful() && result.hasBody()) {
                    return ResponseFactory.responseOk(tenant);
                } else {
                    throw new KmsTenantUpdateException("for tenant id: " + id);
                }
            } catch (Exception e) {
                log.error("Remote feign call failed : ", e);
                //throw new RemoteCallFailedException(e);
            }
            return ResponseFactory.responseOk(tenant);
        } catch (Throwable e) {
            log.error("<Error>: update Domain Status : {} ", e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<List<String>> getAllDomainNames(ContextRequestDto requestContext) {
        log.info("getAllDomainNames {}", requestContext.getSenderTenant());
        try {
            return ResponseFactory.responseOk(crudService().getAllDomainNames(requestContext.getSenderTenant()));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<TenantDto> getByName(ContextRequestDto requestContext) {
        log.info("get by name {}", requestContext.getSenderTenant());
        try {
            return ResponseFactory.responseOk(mapper().entityToDto(crudService().findByName(requestContext.getSenderTenant())));
        } catch (Throwable e) {
            log.error("<Error>: get by name : {} ", e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<TenantDto> updateSocialLink(ContextRequestDto requestContext, Long id, String social, String link) {
        log.info("update Social Link ");
        try {
            return ResponseFactory.responseOk(mapper().entityToDto(crudService().updateSocialLink(requestContext.getSenderTenant(), id, social, link)));
        } catch (Throwable e) {
            log.error("<Error>: update Social Link : {} ", e);
            return getBackExceptionResponse(e);
        }
    }
}

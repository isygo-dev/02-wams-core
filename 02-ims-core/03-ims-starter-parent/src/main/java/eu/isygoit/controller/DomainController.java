package eu.isygoit.controller;

import eu.isygoit.annotation.InjectMapperAndService;
import eu.isygoit.api.DomainControllerApi;
import eu.isygoit.com.rest.controller.ResponseFactory;
import eu.isygoit.com.rest.controller.constants.CtrlConstants;
import eu.isygoit.com.rest.controller.impl.MappedCrudController;
import eu.isygoit.com.rest.controller.impl.tenancy.MappedCrudTenantController;
import eu.isygoit.dto.common.RequestContextDto;
import eu.isygoit.dto.data.DomainDto;
import eu.isygoit.dto.data.KmsDomainDto;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.exception.KmsDomainUpdateException;
import eu.isygoit.exception.handler.ImsExceptionHandler;
import eu.isygoit.mapper.DomainMapper;
import eu.isygoit.model.Domain;
import eu.isygoit.remote.kms.KmsDomainService;
import eu.isygoit.service.impl.DomainService;
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
@InjectMapperAndService(handler = ImsExceptionHandler.class, mapper = DomainMapper.class, minMapper = DomainMapper.class, service = DomainService.class)
@RequestMapping(path = "/api/v1/private/tenant")
public class DomainController extends MappedCrudTenantController<Long, Domain, DomainDto, DomainDto, DomainService>
        implements DomainControllerApi {

    @Autowired
    private KmsDomainService kmsDomainService;

    @Override
    public Domain afterUpdate(Domain tenant) {
        try {
            ResponseEntity<Boolean> result = kmsDomainService.updateDomain(//RequestContextDto.builder().build(),
                    KmsDomainDto.builder()
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

        throw new KmsDomainUpdateException("for tenant id: " + tenant.getId());
    }

    @Override
    public ResponseEntity<DomainDto> updateAdminStatus(RequestContextDto requestContext,
                                                       Long id,
                                                       IEnumEnabledBinaryStatus.Types newStatus) {
        log.info("in update status");
        try {
            DomainDto tenant = mapper().entityToDto(crudService().updateAdminStatus(id, newStatus));
            try {
                ResponseEntity<KmsDomainDto> result = kmsDomainService.updateAdminStatus(RequestContextDto.builder().build(),
                        tenant.getName(), newStatus);
                if (result.getStatusCode().is2xxSuccessful() && result.hasBody()) {
                    return ResponseFactory.responseOk(tenant);
                } else {
                    throw new KmsDomainUpdateException("for tenant id: " + id);
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
    public ResponseEntity<List<String>> getAllDomainNames(RequestContextDto requestContext) {
        log.info("getAllDomainNames {}", requestContext.getSenderTenant());
        try {
            return ResponseFactory.responseOk(crudService().getAllDomainNames(requestContext.getSenderTenant()));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<DomainDto> getByName(RequestContextDto requestContext) {
        log.info("get by name {}", requestContext.getSenderTenant());
        try {
            return ResponseFactory.responseOk(mapper().entityToDto(crudService().findByName(requestContext.getSenderTenant())));
        } catch (Throwable e) {
            log.error("<Error>: get by name : {} ", e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<DomainDto> updateSocialLink(RequestContextDto requestContext, Long id, String social, String link) {
        log.info("update Social Link ");
        try {
            return ResponseFactory.responseOk(mapper().entityToDto(crudService().updateSocialLink(requestContext.getSenderTenant(), id, social, link)));
        } catch (Throwable e) {
            log.error("<Error>: update Social Link : {} ", e);
            return getBackExceptionResponse(e);
        }
    }
}

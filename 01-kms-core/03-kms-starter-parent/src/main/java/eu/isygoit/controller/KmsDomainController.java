package eu.isygoit.controller;

import eu.isygoit.annotation.CtrlDef;
import eu.isygoit.api.KmsDomainControllerApi;
import eu.isygoit.app.ApplicationContextService;
import eu.isygoit.com.rest.controller.ResponseFactory;
import eu.isygoit.com.rest.controller.constants.CtrlConstants;
import eu.isygoit.com.rest.controller.impl.MappedCrudController;
import eu.isygoit.dto.common.RequestContextDto;
import eu.isygoit.dto.data.KmsDomainDto;
import eu.isygoit.enums.IEnumBinaryStatus;
import eu.isygoit.exception.handler.KmsExceptionHandler;
import eu.isygoit.mapper.DomainMapper;
import eu.isygoit.model.KmsDomain;
import eu.isygoit.service.impl.DomainService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The type Kms domain controller.
 */
@Slf4j
@Validated
@RestController
@CtrlDef(handler = KmsExceptionHandler.class, mapper = DomainMapper.class, minMapper = DomainMapper.class, service = DomainService.class)
@RequestMapping(path = "/api/v1/private/domain")
public class KmsDomainController extends MappedCrudController<Long, KmsDomain, KmsDomainDto, KmsDomainDto, DomainService>
        implements KmsDomainControllerApi {

    private final ApplicationContextService applicationContextService;

    @Autowired
    public KmsDomainController(ApplicationContextService applicationContextService) {
        this.applicationContextService = applicationContextService;
    }

    @Override
    protected ApplicationContextService getApplicationContextServiceInstance() {
        return applicationContextService;
    }

    @Override
    public ResponseEntity<KmsDomainDto> updateAdminStatus(RequestContextDto requestContext,
                                                          String domain,
                                                          IEnumBinaryStatus.Types newStatus) {
        log.info("in update status");
        try {
            return crudService().updateAdminStatus(domain, newStatus)
                    .map(kmsDomain -> ResponseFactory.ResponseOk(mapper().entityToDto(kmsDomain)))
                    .orElse(ResponseFactory.ResponseNotFound());
        } catch (Throwable e) {
            log.error("<Error>: update Domain Status : {} ", e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<KmsDomainDto> updateDomain(//RequestContextDto requestContext,
                                                     KmsDomainDto domain) {
        log.info("Call update domain " + domain.toString());
        try {
            return crudService().checkIfExists(mapper().dtoToEntity(domain), true)
                    .map(kmsDomain -> ResponseFactory.ResponseOk(mapper().entityToDto(kmsDomain)))
                    .orElse(ResponseFactory.ResponseNotFound());
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }
}

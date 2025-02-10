package eu.isygoit.controller;

import eu.isygoit.annotation.CtrlDef;
import eu.isygoit.app.ApplicationContextService;
import eu.isygoit.com.rest.controller.impl.MappedImageController;
import eu.isygoit.dto.data.DomainDto;
import eu.isygoit.dto.data.KmsDomainDto;
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

import javax.security.auth.login.AccountException;

/**
 * The type Domain image controller.
 */
@Slf4j
@Validated
@RestController
@CtrlDef(handler = ImsExceptionHandler.class, mapper = DomainMapper.class, minMapper = DomainMapper.class, service = DomainService.class)
@RequestMapping(path = "/api/v1/private/domain")
public class DomainImageController extends MappedImageController<Long, Domain, DomainDto, DomainDto, DomainService> {

    private final ApplicationContextService applicationContextService;
    private final KmsDomainService kmsDomainService;

    @Autowired
    public DomainImageController(ApplicationContextService applicationContextService, KmsDomainService kmsDomainService) {
        this.applicationContextService = applicationContextService;
        this.kmsDomainService = kmsDomainService;
    }

    @Override
    protected ApplicationContextService getApplicationContextServiceInstance() {
        return applicationContextService;
    }

    @Override
    public Domain afterUpdate(Domain domain) throws Exception {
        try {
            ResponseEntity<KmsDomainDto> result = kmsDomainService.updateDomain(//RequestContextDto.builder().build(),
                    KmsDomainDto.builder()
                            .name(domain.getName())
                            .description(domain.getDescription())
                            .url(domain.getUrl())
                            .adminStatus(domain.getAdminStatus())
                            .build());
            if (result.getStatusCode().is2xxSuccessful() && result.hasBody()) {
                return super.afterUpdate(domain);
            }
        } catch (Exception e) {
            log.error("Remote feign call failed : ", e);
            //throw new RemoteCallFailedException(e);
        }

        throw new AccountException("Update domain issue in KMS");
    }
}

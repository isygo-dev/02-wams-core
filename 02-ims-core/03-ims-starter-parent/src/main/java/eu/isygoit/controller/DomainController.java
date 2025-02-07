package eu.isygoit.controller;

import eu.isygoit.annotation.CtrlDef;
import eu.isygoit.api.DomainControllerApi;
import eu.isygoit.com.rest.controller.ResponseFactory;
import eu.isygoit.com.rest.controller.constants.CtrlConstants;
import eu.isygoit.com.rest.controller.impl.MappedCrudController;
import eu.isygoit.dto.common.RequestContextDto;
import eu.isygoit.dto.data.DomainDto;
import eu.isygoit.dto.data.KmsDomainDto;
import eu.isygoit.enums.IEnumBinaryStatus;
import eu.isygoit.exception.DomainNotFoundException;
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
@CtrlDef(handler = ImsExceptionHandler.class, mapper = DomainMapper.class, minMapper = DomainMapper.class, service = DomainService.class)
@RequestMapping(path = "/api/v1/private/domain")
public class DomainController extends MappedCrudController<Long, Domain, DomainDto, DomainDto, DomainService>
        implements DomainControllerApi {

    private final KmsDomainService kmsDomainService;

    @Autowired
    public DomainController(KmsDomainService kmsDomainService) {
        this.kmsDomainService = kmsDomainService;
    }

    @Override
    public Domain afterUpdate(Domain domain) {
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

        throw new KmsDomainUpdateException("for domain id: " + domain.getId());
    }

    @Override
    public ResponseEntity<DomainDto> updateAdminStatus(RequestContextDto requestContext,
                                                       Long id,
                                                       IEnumBinaryStatus.Types newStatus) {
        log.info("in update status");
        try {
            DomainDto domain = mapper().entityToDto(crudService().updateAdminStatus(id, newStatus));
            try {
                ResponseEntity<KmsDomainDto> result = kmsDomainService.updateAdminStatus(RequestContextDto.builder().build(),
                        domain.getName(), newStatus);
                if (result.getStatusCode().is2xxSuccessful() && result.hasBody()) {
                    return ResponseFactory.ResponseOk(domain);
                } else {
                    throw new KmsDomainUpdateException("for domain id: " + id);
                }
            } catch (Exception e) {
                log.error("Remote feign call failed : ", e);
                //throw new RemoteCallFailedException(e);
            }
            return ResponseFactory.ResponseOk(domain);
        } catch (Throwable e) {
            log.error("<Error>: update Domain Status : {} ", e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<List<String>> getAllDomainNames(RequestContextDto requestContext) {
        log.info("getAllDomainNames {}", requestContext.getSenderDomain());
        try {
            return ResponseFactory.ResponseOk(crudService().getAllDomainNames(requestContext.getSenderDomain()));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<DomainDto> getByName(RequestContextDto requestContext) {
        log.info("get by name {}", requestContext.getSenderDomain());
        try {
            return ResponseFactory.ResponseOk(mapper().entityToDto(crudService().findByName(requestContext.getSenderDomain())
                    .orElseThrow(() -> new DomainNotFoundException("with name " + requestContext.getSenderDomain()))));
        } catch (Throwable e) {
            log.error("<Error>: get by name : {} ", e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<DomainDto> updateSocialLink(RequestContextDto requestContext, Long id, String social, String link) {
        log.info("update Social Link ");
        try {
            return ResponseFactory.ResponseOk(mapper().entityToDto(crudService().updateSocialLink(requestContext.getSenderDomain(), id, social, link)));
        } catch (Throwable e) {
            log.error("<Error>: update Social Link : {} ", e);
            return getBackExceptionResponse(e);
        }
    }
}

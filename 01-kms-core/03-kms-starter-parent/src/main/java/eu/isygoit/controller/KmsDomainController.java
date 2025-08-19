package eu.isygoit.controller;

import eu.isygoit.annotation.InjectMapperAndService;
import eu.isygoit.api.KmsDomainControllerApi;
import eu.isygoit.com.rest.controller.ResponseFactory;
import eu.isygoit.com.rest.controller.constants.CtrlConstants;
import eu.isygoit.com.rest.controller.impl.MappedCrudController;
import eu.isygoit.dto.common.ContextRequestDto;
import eu.isygoit.dto.data.KmsDomainDto;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.exception.handler.KmsExceptionHandler;
import eu.isygoit.mapper.DomainMapper;
import eu.isygoit.model.KmsDomain;
import eu.isygoit.service.impl.DomainService;
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
@RestController
@InjectMapperAndService(handler = KmsExceptionHandler.class, mapper = DomainMapper.class, minMapper = DomainMapper.class, service = DomainService.class)
@RequestMapping(path = "/api/v1/private/tenant")
public class KmsDomainController extends MappedCrudController<Long, KmsDomain, KmsDomainDto, KmsDomainDto, DomainService>
        implements KmsDomainControllerApi {

    @Override
    public ResponseEntity<KmsDomainDto> updateAdminStatus(ContextRequestDto requestContext,
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
    public ResponseEntity<Boolean> updateDomain(ContextRequestDto requestContext,
                                                KmsDomainDto tenant) {
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

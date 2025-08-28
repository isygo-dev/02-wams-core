package eu.isygoit.controller;

import eu.isygoit.annotation.InjectMapperAndService;
import eu.isygoit.api.KmsTenantControllerApi;
import eu.isygoit.com.rest.controller.ResponseFactory;
import eu.isygoit.com.rest.controller.constants.CtrlConstants;
import eu.isygoit.com.rest.controller.impl.MappedCrudController;
import eu.isygoit.dto.common.ContextRequestDto;
import eu.isygoit.dto.data.KmsTenantDto;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.exception.handler.KmsExceptionHandler;
import eu.isygoit.mapper.TenantMapper;
import eu.isygoit.model.Tenant;
import eu.isygoit.service.impl.TenantService;
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
@InjectMapperAndService(handler = KmsExceptionHandler.class, mapper = TenantMapper.class, minMapper = TenantMapper.class, service = TenantService.class)
@RequestMapping(path = "/api/v1/private/tenant")
public class KmsTenantController extends MappedCrudController<Long, Tenant, KmsTenantDto, KmsTenantDto, TenantService>
        implements KmsTenantControllerApi {

    @Override
    public ResponseEntity<KmsTenantDto> updateAdminStatus(ContextRequestDto requestContext,
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

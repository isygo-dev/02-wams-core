package eu.isygoit.controller;

import eu.isygoit.annotation.InjectMapperAndService;
import eu.isygoit.api.AppParameterControllerApi;
import eu.isygoit.com.rest.controller.ResponseFactory;
import eu.isygoit.com.rest.controller.impl.MappedCrudController;
import eu.isygoit.com.rest.controller.impl.tenancy.MappedCrudTenantController;
import eu.isygoit.dto.common.RequestContextDto;
import eu.isygoit.dto.data.AppParameterDto;
import eu.isygoit.exception.handler.ImsExceptionHandler;
import eu.isygoit.mapper.AppParameterMapper;
import eu.isygoit.model.AppParameter;
import eu.isygoit.service.impl.AppParameterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The type App parameter controller.
 */
@Slf4j
@Validated
@RestController
@InjectMapperAndService(handler = ImsExceptionHandler.class, mapper = AppParameterMapper.class, minMapper = AppParameterMapper.class, service = AppParameterService.class)
@RequestMapping(path = "/api/v1/private/appParameter")
public class AppParameterController extends MappedCrudTenantController<Long, AppParameter, AppParameterDto, AppParameterDto, AppParameterService>
        implements AppParameterControllerApi {

    @Override
    public ResponseEntity<String> getValueByTenantAndName(RequestContextDto requestContext,
                                                          String tenant, String name, Boolean allowDefault, String defaultValue) {
        log.info("Call api getPropertyByAccount {} /{}", tenant, name);
        try {
            return ResponseFactory.responseOk(crudService().getValueByTenantAndName(tenant, name, true, defaultValue));
        } catch (Throwable e) {
            log.error("<Error>: Call api getPropertyByAccount {} /{} {}", tenant, name, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<String> getTechnicalAdminEmail() {
        log.info("Call api getTechnicalAdminEmail");
        try {
            return ResponseFactory.responseOk(crudService().getTechnicalAdminEmail());
        } catch (Throwable e) {
            log.error("<Error>: Call api getTechnicalAdminEmail", e);
            return getBackExceptionResponse(e);
        }
    }
}

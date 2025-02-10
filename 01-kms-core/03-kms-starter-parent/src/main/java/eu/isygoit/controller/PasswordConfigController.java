package eu.isygoit.controller;

import eu.isygoit.annotation.CtrlDef;
import eu.isygoit.app.ApplicationContextService;
import eu.isygoit.com.rest.controller.impl.MappedCrudController;
import eu.isygoit.dto.data.PasswordConfigDto;
import eu.isygoit.exception.handler.KmsExceptionHandler;
import eu.isygoit.mapper.PasswordConfigMapper;
import eu.isygoit.model.PasswordConfig;
import eu.isygoit.service.impl.PasswordConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The type Password config controller.
 */
@Slf4j
@Validated
@RestController
@CtrlDef(handler = KmsExceptionHandler.class, mapper = PasswordConfigMapper.class, minMapper = PasswordConfigMapper.class, service = PasswordConfigService.class)
@RequestMapping(path = "/api/v1/private/config/password")
public class PasswordConfigController extends MappedCrudController<Long, PasswordConfig, PasswordConfigDto, PasswordConfigDto, PasswordConfigService> {

    private final ApplicationContextService applicationContextService;

    @Autowired
    public PasswordConfigController(ApplicationContextService applicationContextService) {
        this.applicationContextService = applicationContextService;
    }

    @Override
    protected ApplicationContextService getApplicationContextServiceInstance() {
        return applicationContextService;
    }
}

package eu.isygoit.controller;

import eu.isygoit.annotation.CtrlDef;
import eu.isygoit.app.ApplicationContextService;
import eu.isygoit.com.rest.controller.impl.MappedCrudController;
import eu.isygoit.dto.common.NextCodeDto;
import eu.isygoit.exception.handler.KmsExceptionHandler;
import eu.isygoit.mapper.AppNextCodeMapper;
import eu.isygoit.model.AppNextCode;
import eu.isygoit.service.impl.AppNextCodeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The type App next code controller.
 */
@Slf4j
@Validated
@RestController
@CtrlDef(handler = KmsExceptionHandler.class, mapper = AppNextCodeMapper.class, minMapper = AppNextCodeMapper.class, service = AppNextCodeService.class)
@RequestMapping(path = "/api/v1/private/code")
public class AppNextCodeController extends MappedCrudController<Long, AppNextCode, NextCodeDto, NextCodeDto, AppNextCodeService> {

    private final ApplicationContextService applicationContextService;

    @Autowired
    public AppNextCodeController(ApplicationContextService applicationContextService) {
        this.applicationContextService = applicationContextService;
    }

    @Override
    protected ApplicationContextService getApplicationContextServiceInstance() {
        return applicationContextService;
    }
}

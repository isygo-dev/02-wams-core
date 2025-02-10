package eu.isygoit.controller;

import eu.isygoit.annotation.CtrlDef;
import eu.isygoit.app.ApplicationContextService;
import eu.isygoit.com.rest.controller.impl.MappedCrudController;
import eu.isygoit.dto.data.DigestConfigDto;
import eu.isygoit.exception.handler.KmsExceptionHandler;
import eu.isygoit.mapper.DigestConfigMapper;
import eu.isygoit.model.DigestConfig;
import eu.isygoit.service.impl.DigestConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The type Digest config controller.
 */
@Slf4j
@Validated
@RestController
@CtrlDef(handler = KmsExceptionHandler.class, mapper = DigestConfigMapper.class, minMapper = DigestConfigMapper.class, service = DigestConfigService.class)
@RequestMapping(path = "/api/v1/private/config/digest")
public class DigestConfigController extends MappedCrudController<Long, DigestConfig, DigestConfigDto, DigestConfigDto, DigestConfigService> {

    private final ApplicationContextService applicationContextService;

    @Autowired
    public DigestConfigController(ApplicationContextService applicationContextService) {
        this.applicationContextService = applicationContextService;
    }

    @Override
    protected ApplicationContextService getApplicationContextServiceInstance() {
        return applicationContextService;
    }
}

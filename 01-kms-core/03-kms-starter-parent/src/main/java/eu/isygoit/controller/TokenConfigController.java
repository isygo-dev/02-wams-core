package eu.isygoit.controller;

import eu.isygoit.annotation.CtrlDef;
import eu.isygoit.app.ApplicationContextService;
import eu.isygoit.com.rest.controller.impl.MappedCrudController;
import eu.isygoit.dto.data.TokenConfigDto;
import eu.isygoit.exception.handler.KmsExceptionHandler;
import eu.isygoit.mapper.TokenConfigMapper;
import eu.isygoit.model.TokenConfig;
import eu.isygoit.service.impl.TokenConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The type Token config controller.
 */
@Slf4j
@Validated
@RestController
@RequestMapping(path = "/api/v1/private/config/token")
@CtrlDef(handler = KmsExceptionHandler.class, mapper = TokenConfigMapper.class, minMapper = TokenConfigMapper.class, service = TokenConfigService.class)
public class TokenConfigController extends MappedCrudController<Long, TokenConfig, TokenConfigDto, TokenConfigDto, TokenConfigService> {

    private final ApplicationContextService applicationContextService;

    @Autowired
    public TokenConfigController(ApplicationContextService applicationContextService) {
        this.applicationContextService = applicationContextService;
    }

    @Override
    protected ApplicationContextService getApplicationContextServiceInstance() {
        return applicationContextService;
    }
}

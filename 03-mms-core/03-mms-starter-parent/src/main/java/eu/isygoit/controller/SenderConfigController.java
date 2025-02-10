package eu.isygoit.controller;

import eu.isygoit.annotation.CtrlDef;
import eu.isygoit.app.ApplicationContextService;
import eu.isygoit.com.rest.controller.impl.MappedCrudController;
import eu.isygoit.dto.data.SenderConfigDto;
import eu.isygoit.exception.handler.MmsExceptionHandler;
import eu.isygoit.factory.SenderFactory;
import eu.isygoit.mapper.SenderConfigMapper;
import eu.isygoit.model.SenderConfig;
import eu.isygoit.service.impl.SenderConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The type Sender config controller.
 */
//http://localhost:8060/webjars/swagger-ui/index.html#/
//http://localhost:8060/messaging/mms/private/account
@Slf4j
@Validated
@RestController
@RequestMapping(path = "/api/v1/private/config/mail")
@CtrlDef(handler = MmsExceptionHandler.class, mapper = SenderConfigMapper.class, minMapper = SenderConfigMapper.class, service = SenderConfigService.class)
public class SenderConfigController extends MappedCrudController<Long, SenderConfig, SenderConfigDto, SenderConfigDto, SenderConfigService> {

    private final ApplicationContextService applicationContextService;
    private final SenderFactory senderFactory;

    @Autowired
    public SenderConfigController(ApplicationContextService applicationContextService, SenderFactory senderFactory) {
        this.applicationContextService = applicationContextService;
        this.senderFactory = senderFactory;
    }

    @Override
    protected ApplicationContextService getApplicationContextServiceInstance() {
        return applicationContextService;
    }

    @Override
    public SenderConfig afterUpdate(SenderConfig senderConfig) {
        senderFactory.removeSender(senderConfig.getDomain());
        return super.afterUpdate(senderConfig);
    }
}

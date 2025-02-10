package eu.isygoit.controller;

import eu.isygoit.annotation.CtrlHandler;
import eu.isygoit.api.IncrementalKeyControllerApi;
import eu.isygoit.app.ApplicationContextService;
import eu.isygoit.com.rest.controller.ResponseFactory;
import eu.isygoit.com.rest.controller.constants.CtrlConstants;
import eu.isygoit.com.rest.controller.impl.ControllerExceptionHandler;
import eu.isygoit.dto.common.NextCodeDto;
import eu.isygoit.dto.common.RequestContextDto;
import eu.isygoit.exception.handler.KmsExceptionHandler;
import eu.isygoit.model.AppNextCode;
import eu.isygoit.service.IKeyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * The type Incremental key controller.
 */
@Slf4j
@Validated
@RestController
@CtrlHandler(KmsExceptionHandler.class)
@RequestMapping(path = "/api/v1/private/key")
public class IncrementalKeyController extends ControllerExceptionHandler implements IncrementalKeyControllerApi {

    private final ApplicationContextService applicationContextService;
    private final IKeyService keyService;

    @Autowired
    public IncrementalKeyController(ApplicationContextService applicationContextService, IKeyService keyService) {
        this.applicationContextService = applicationContextService;
        this.keyService = keyService;
    }

    @Override
    protected ApplicationContextService getApplicationContextServiceInstance() {
        return applicationContextService;
    }

    @Override
    public ResponseEntity<String> generateNextCode(RequestContextDto requestContext,
                                                   String domain, String entity, String attribute) {
        log.info("Call generate next code for: {}/{}/{}", domain, entity, attribute);
        try {
            return ResponseFactory.ResponseOk(keyService.getIncrementalKey(domain, entity, attribute));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<String> subscribeNextCode(//RequestContextDto requestContext,
                                                    String domain, NextCodeDto incrementalConfig) {
        log.info("Call subscribe next code generator for: {}/{}", domain, incrementalConfig);
        try {
            keyService.subscribeIncrementalKeyGenerator(AppNextCode.builder()
                    .domain(domain)
                    .entity(incrementalConfig.getEntity())
                    .attribute(incrementalConfig.getAttribute())
                    .prefix(incrementalConfig.getPrefix())
                    .suffix(incrementalConfig.getSuffix())
                    .valueLength(incrementalConfig.getValueLength())
                    .value(incrementalConfig.getValue())
                    .increment(incrementalConfig.getIncrement())
                    .build());

            return ResponseFactory.ResponseOk();
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }
}
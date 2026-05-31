package eu.isygoit.controller;

import eu.isygoit.annotation.InjectExceptionHandler;
import eu.isygoit.api.IncrementalKeyServiceApi;
import eu.isygoit.com.rest.controller.ResponseFactory;
import eu.isygoit.com.rest.controller.constants.CtrlConstants;
import eu.isygoit.com.rest.controller.impl.ControllerExceptionHandler;
import eu.isygoit.dto.common.NextCodeDto;
import eu.isygoit.exception.handler.KmsExceptionHandler;
import eu.isygoit.model.AppNextCode;
import eu.isygoit.service.IKeyService;
import eu.isygoit.service.RequestContextService;
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
@InjectExceptionHandler(KmsExceptionHandler.class)
@RequestMapping(path = "/api/v1/private/key")
public class IncrementalKeyController extends ControllerExceptionHandler implements IncrementalKeyServiceApi {

    @Autowired
    private IKeyService keyService;

    @Autowired
    private RequestContextService requestContextService;

    @Override
    public ResponseEntity<String> generateNextCode(String entity, String attribute) {
        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        log.info("Call generate next code for: {}/{}/{}", tenant, entity, attribute);
        try {
            return ResponseFactory.responseOk(keyService.getIncrementalKey(tenant, entity, attribute));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<String> subscribeNextCode(NextCodeDto incrementalConfig) {
        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        log.info("Call subscribe next code generator for: {}/{}", tenant, incrementalConfig);
        try {
            keyService.subscribeIncrementalKeyGenerator(AppNextCode.builder()
                    .tenant(tenant)
                    .entity(incrementalConfig.getEntity())
                    .attribute(incrementalConfig.getAttribute())
                    .prefix(incrementalConfig.getPrefix())
                    .suffix(incrementalConfig.getSuffix())
                    .valueLength(incrementalConfig.getValueLength())
                    .codeValue(incrementalConfig.getCodeValue())
                    .increment(incrementalConfig.getIncrement())
                    .build());

            return ResponseFactory.responseOk();
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }
}
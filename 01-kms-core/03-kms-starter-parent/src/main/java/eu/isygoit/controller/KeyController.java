package eu.isygoit.controller;

import eu.isygoit.annotation.InjectExceptionHandler;
import eu.isygoit.com.rest.controller.ResponseFactory;
import eu.isygoit.com.rest.controller.constants.CtrlConstants;
import eu.isygoit.com.rest.controller.impl.ControllerExceptionHandler;
import eu.isygoit.dto.common.RequestContextDto;
import eu.isygoit.enums.IEnumCharSet;
import eu.isygoit.exception.handler.KmsExceptionHandler;
import eu.isygoit.service.IKeyService;
import eu.isygoit.service.KeyServiceApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * The type Key controller.
 */
@Slf4j
@Validated
@RestController
@InjectExceptionHandler(KmsExceptionHandler.class)
@RequestMapping(path = "/api/v1/private/key")
public class KeyController extends ControllerExceptionHandler implements KeyServiceApi {

    @Autowired
    private IKeyService keyService;

    @Override
    public ResponseEntity<String> newRandomKey(RequestContextDto requestContext,
                                               Integer length, IEnumCharSet.Types charSetType) {
        log.info("Call generateRandomKey");
        try {
            return ResponseFactory.responseOk(keyService.getRandomKey(length, charSetType));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<String> renewRandomKey(RequestContextDto requestContext,
                                                 String tenant, String keyName, Integer length, IEnumCharSet.Types charSetType) {
        log.info("Call generateRandomKeyName");
        try {
            String keyValue = keyService.getRandomKey(length, charSetType);
            keyService.createOrUpdateKeyByName(tenant, keyName, keyValue);
            return ResponseFactory.responseOk(keyValue);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<String> getRandomKey(RequestContextDto requestContext,
                                               String tenant, String keyName) {
        log.info("Call getRandomKeyName");
        try {
            return ResponseFactory.responseOk(keyService.getKeyByName(tenant, keyName).getValue());
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }
}
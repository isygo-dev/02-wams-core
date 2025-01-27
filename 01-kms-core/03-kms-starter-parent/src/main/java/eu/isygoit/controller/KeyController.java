package eu.isygoit.controller;

import eu.isygoit.annotation.CtrlHandler;
import eu.isygoit.com.rest.controller.ResponseFactory;
import eu.isygoit.com.rest.controller.constants.CtrlConstants;
import eu.isygoit.com.rest.controller.impl.ControllerExceptionHandler;
import eu.isygoit.dto.common.RequestContextDto;
import eu.isygoit.enums.IEnumCharSet;
import eu.isygoit.exception.RandomKeyNotFoundException;
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
@CtrlHandler(KmsExceptionHandler.class)
@RequestMapping(path = "/api/v1/private/key")
public class KeyController extends ControllerExceptionHandler implements KeyServiceApi {

    @Autowired
    private IKeyService keyService;

    @Override
    public ResponseEntity<String> generateRandomKey(RequestContextDto requestContext,
                                                    Integer length, IEnumCharSet.Types charSetType) {
        log.info("Call generateRandomKey");
        try {
            return ResponseFactory.ResponseOk(keyService.getRandomKey(length, charSetType));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<String> renewKeyByName(RequestContextDto requestContext,
                                                 String domain, String keyName, Integer length, IEnumCharSet.Types charSetType) {
        log.info("Call generateRandomKeyName");
        try {
            String keyValue = keyService.getRandomKey(length, charSetType);
            keyService.createOrUpdateKeyByName(domain, keyName, keyValue);
            return ResponseFactory.ResponseOk(keyValue);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<String> getKeyByName(RequestContextDto requestContext,
                                               String domain, String keyName) {
        log.info("Call getRandomKeyName");
        try {
            return ResponseFactory.ResponseOk(keyService.getKeyByName(domain, keyName)
                    .orElseThrow(() -> new RandomKeyNotFoundException("for domain " + domain + " and name " + keyName)).getValue());
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }
}
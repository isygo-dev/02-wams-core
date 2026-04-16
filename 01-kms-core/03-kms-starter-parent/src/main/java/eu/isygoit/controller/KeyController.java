package eu.isygoit.controller;

import eu.isygoit.annotation.InjectExceptionHandler;
import eu.isygoit.com.rest.controller.ResponseFactory;
import eu.isygoit.com.rest.controller.constants.CtrlConstants;
import eu.isygoit.com.rest.controller.impl.ControllerExceptionHandler;
import eu.isygoit.dto.common.ContextRequestDto;
import eu.isygoit.enums.IEnumCharSet;
import eu.isygoit.exception.handler.KmsExceptionHandler;
import eu.isygoit.service.IKeyService;
import eu.isygoit.service.KeyServiceApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

    @Operation(summary = "New random key Api",
            description = "New random key")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = String.class))})
    })
    @Override
    public ResponseEntity<String> newRandomKey(ContextRequestDto requestContext,
                                               Integer length, IEnumCharSet.Types charSetType) {
        log.info("Call generateRandomKey");
        try {
            return ResponseFactory.responseOk(keyService.getRandomKey(length, charSetType));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Operation(summary = "Renew random key Api",
            description = "Renew random key")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = String.class))})
    })
    @Override
    public ResponseEntity<String> renewRandomKey(ContextRequestDto requestContext,
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

    @Operation(summary = "Get random key Api",
            description = "Get random key")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = String.class))})
    })
    @Override
    public ResponseEntity<String> getRandomKey(ContextRequestDto requestContext,
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
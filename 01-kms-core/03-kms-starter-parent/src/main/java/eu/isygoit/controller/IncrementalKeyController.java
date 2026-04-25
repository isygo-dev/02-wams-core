package eu.isygoit.controller;

import eu.isygoit.annotation.InjectExceptionHandler;
import eu.isygoit.api.IncrementalKeyControllerApi;
import eu.isygoit.com.rest.controller.ResponseFactory;
import eu.isygoit.com.rest.controller.constants.CtrlConstants;
import eu.isygoit.com.rest.controller.impl.ControllerExceptionHandler;
import eu.isygoit.dto.common.NextCodeDto;
import eu.isygoit.exception.handler.KmsExceptionHandler;
import eu.isygoit.model.AppNextCode;
import eu.isygoit.service.IKeyService;
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
 * The type Incremental key controller.
 */
@Slf4j
@Validated
@RestController
@InjectExceptionHandler(KmsExceptionHandler.class)
@RequestMapping(path = "/api/v1/private/key")
public class IncrementalKeyController extends ControllerExceptionHandler implements IncrementalKeyControllerApi {

    @Autowired
    private IKeyService keyService;


    @Operation(summary = "Generate next code Api",
            description = "Generate next code")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = String.class))})
    })
    @Override
    public ResponseEntity<String> generateNextCode(
            String tenant, String entity, String attribute) {
        log.info("Call generate next code for: {}/{}/{}", tenant, entity, attribute);
        try {
            return ResponseFactory.responseOk(keyService.getIncrementalKey(tenant, entity, attribute));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Operation(summary = "Subscribe next code Api",
            description = "Subscribe next code")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = String.class))})
    })
    @Override
    public ResponseEntity<String> subscribeNextCode(
            String tenant, NextCodeDto incrementalConfig) {
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
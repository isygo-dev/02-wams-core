package eu.isygoit.controller;

import eu.isygoit.annotation.InjectExceptionHandler;
import eu.isygoit.api.PublicPasswordControllerApi;
import eu.isygoit.com.rest.controller.ResponseFactory;
import eu.isygoit.com.rest.controller.constants.CtrlConstants;
import eu.isygoit.com.rest.controller.impl.ControllerExceptionHandler;
import eu.isygoit.dto.common.UserContextDto;
import eu.isygoit.exception.handler.KmsExceptionHandler;
import eu.isygoit.service.ITokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The type Public password controller.
 */
@Slf4j
@Validated
@RestController
@InjectExceptionHandler(KmsExceptionHandler.class)
@RequestMapping(path = "/api/v1/public/password")
public class PublicPasswordController extends ControllerExceptionHandler implements PublicPasswordControllerApi {

    @Autowired
    private ITokenService tokenService;

    @Override
    public ResponseEntity<Boolean> generateForgotPasswordAccessToken(UserContextDto userContextDto) {
        log.info("Call generateForgotPasswordAccessToken " + userContextDto.toString());
        try {
            tokenService.createForgotPasswordAccessToken(userContextDto.getTenant(),
                    userContextDto.getApplication(),
                    userContextDto.getUserName());
            return ResponseFactory.responseOk(true);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }
}

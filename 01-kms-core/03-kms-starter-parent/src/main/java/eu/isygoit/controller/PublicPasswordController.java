package eu.isygoit.controller;

import eu.isygoit.annotation.InjectExceptionHandler;
import eu.isygoit.api.PublicPasswordControllerApi;
import eu.isygoit.com.rest.controller.ResponseFactory;
import eu.isygoit.com.rest.controller.constants.CtrlConstants;
import eu.isygoit.com.rest.controller.impl.ControllerExceptionHandler;
import eu.isygoit.dto.common.UserContextRequestDto;
import eu.isygoit.exception.handler.KmsExceptionHandler;
import eu.isygoit.service.ITokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Public Password", description = "Endpoints for public password operations")
@RestController
@InjectExceptionHandler(KmsExceptionHandler.class)
@RequestMapping(path = "/api/v1/public/password")
public class PublicPasswordController extends ControllerExceptionHandler implements PublicPasswordControllerApi {

    @Autowired
    private ITokenService tokenService;

    @Operation(summary = "Generate forgot password access token Api",
            description = "Generate forgot password access token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = Boolean.class))})
    })
    @Override
    public ResponseEntity<Boolean> generateForgotPasswordAccessToken(UserContextRequestDto userContextDto) {
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

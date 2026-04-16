package eu.isygoit.controller;

import eu.isygoit.annotation.InjectExceptionHandler;
import eu.isygoit.com.rest.controller.ResponseFactory;
import eu.isygoit.com.rest.controller.impl.ControllerExceptionHandler;
import eu.isygoit.dto.common.ContextRequestDto;
import eu.isygoit.dto.common.TokenResponseDto;
import eu.isygoit.dto.data.TokenRequestDto;
import eu.isygoit.enums.IEnumToken;
import eu.isygoit.exception.handler.KmsExceptionHandler;
import eu.isygoit.service.ITokenService;
import eu.isygoit.service.TokenServiceApi;
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
 * The type Token controller.
 */
@Slf4j
@Validated
@Tag(name = "Token", description = "Endpoints for managing tokens")
@RestController
@InjectExceptionHandler(KmsExceptionHandler.class)
@RequestMapping(path = "/api/v1/private/token")
public class TokenController extends ControllerExceptionHandler implements TokenServiceApi {

    @Autowired
    private ITokenService tokenService;

    @Operation(summary = "Build token by tenant Api",
            description = "Build token by tenant")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = TokenResponseDto.class))})
    })
    @Override
    public ResponseEntity<TokenResponseDto> buildTokenByTenant(ContextRequestDto requestContext,
                                                               String tenant,
                                                               String application,
                                                               IEnumToken.Types tokenType,
                                                               TokenRequestDto tokenRequestDto) {
        log.info("Call create Token By Domain");
        try {
            return ResponseFactory.responseOk(tokenService.buildTokenAndSave(tenant, application, tokenType, tokenRequestDto.getSubject(), tokenRequestDto.getClaims()));
        } catch (Throwable e) {
            log.error("<Error>: create Token By Domain: {} ", e);
            return getBackExceptionResponse(e);
        }
    }

    @Operation(summary = "Is token valid Api",
            description = "Is token valid")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = Boolean.class))})
    })
    @Override
    public ResponseEntity<Boolean> isTokenValid(ContextRequestDto requestContext,
                                                String tenant,
                                                String application,
                                                IEnumToken.Types tokenType,
                                                String token,
                                                String subject) {
        log.info("Call is Token Valid");
        try {
            return ResponseFactory.responseOk(tokenService.isTokenValid(tenant, application, tokenType, token, subject));
        } catch (Throwable e) {
            log.error("<Error>: Invalid token: {} ", e);
            return getBackExceptionResponse(e);
        }
    }
}
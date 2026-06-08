package eu.isygoit.controller;

import eu.isygoit.annotation.InjectExceptionHandler;
import eu.isygoit.com.rest.controller.ResponseFactory;
import eu.isygoit.com.rest.controller.impl.ControllerExceptionHandler;
import eu.isygoit.dto.common.TokenRequestDto;
import eu.isygoit.dto.common.TokenResponseDto;
import eu.isygoit.enums.IEnumToken;
import eu.isygoit.exception.handler.KmsExceptionHandler;
import eu.isygoit.service.ITokenBuilderService;
import eu.isygoit.service.RequestContextService;
import eu.isygoit.service.TokenServiceApi;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;


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
    private ITokenBuilderService tokenService;

    @Autowired
    private RequestContextService requestContextService;

    @Override
    public ResponseEntity<TokenResponseDto> buildToken(
            Set<String> audience,
            IEnumToken.Types tokenType,
            TokenRequestDto tokenRequestDto) {
        log.info("Call create Token By Tenant");
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            return ResponseFactory.responseOk(tokenService.buildTokenAndSave(tenant, audience, tokenType, tokenRequestDto.getSubject(), tokenRequestDto.getClaims()));
        } catch (Throwable e) {
            log.error("<Error>: create Token By Tenant: {} ", e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<Boolean> isTokenValid(
            Set<String> audience,
            IEnumToken.Types tokenType,
            String token,
            String subject) {
        log.info("Call is Token Valid");
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            return ResponseFactory.responseOk(tokenService.isTokenValid(tenant, audience, tokenType, token, subject));
        } catch (Throwable e) {
            log.error("<Error>: Invalid token: {} ", e);
            return getBackExceptionResponse(e);
        }
    }
}
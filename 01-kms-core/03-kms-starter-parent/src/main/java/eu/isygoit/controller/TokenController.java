package eu.isygoit.controller;

import eu.isygoit.annotation.CtrlHandler;
import eu.isygoit.com.rest.controller.ResponseFactory;
import eu.isygoit.com.rest.controller.impl.ControllerExceptionHandler;
import eu.isygoit.dto.common.RequestContextDto;
import eu.isygoit.dto.common.TokenDto;
import eu.isygoit.dto.data.TokenRequestDto;
import eu.isygoit.enums.IEnumToken;
import eu.isygoit.exception.handler.KmsExceptionHandler;
import eu.isygoit.service.ITokenService;
import eu.isygoit.service.TokenServiceApi;
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
@RestController
@CtrlHandler(KmsExceptionHandler.class)
@RequestMapping(path = "/api/v1/private/token")
public class TokenController extends ControllerExceptionHandler implements TokenServiceApi {

    @Autowired
    private ITokenService tokenService;

    @Override
    public ResponseEntity<TokenDto> buildTokenByDomain(//RequestContextDto requestContext,
                                                       String domain,
                                                       String application,
                                                       IEnumToken.Types tokenType,
                                                       TokenRequestDto tokenRequestDto) {
        log.info("Call create Token By Domain");
        try {
            return ResponseFactory.responseOk(tokenService.buildTokenAndSave(domain, application, tokenType, tokenRequestDto.getSubject(), tokenRequestDto.getClaims()));
        } catch (Throwable e) {
            log.error("<Error>: create Token By Domain: {} ", e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<Boolean> isTokenValid(RequestContextDto requestContext,
                                                String domain,
                                                String application,
                                                IEnumToken.Types tokenType,
                                                String token,
                                                String subject) {
        log.info("Call is Token Valid");
        try {
            return ResponseFactory.responseOk(tokenService.isTokenValid(domain, application, tokenType, token, subject));
        } catch (Throwable e) {
            log.error("<Error>: Invalid token: {} ", e);
            return getBackExceptionResponse(e);
        }
    }
}
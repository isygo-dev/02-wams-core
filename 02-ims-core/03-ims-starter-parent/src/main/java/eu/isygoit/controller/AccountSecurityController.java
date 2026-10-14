package eu.isygoit.controller;

import eu.isygoit.annotation.InjectExceptionHandler;
import eu.isygoit.api.AuthTypeServiceApi;
import eu.isygoit.com.rest.controller.ResponseFactory;
import eu.isygoit.com.rest.controller.constants.CtrlConstants;
import eu.isygoit.com.rest.controller.impl.ControllerUtils;
import eu.isygoit.dto.common.RequestContextDto;
import eu.isygoit.dto.request.ChangeAuthTypeRequestDto;
import eu.isygoit.exception.handler.ImsExceptionHandler;
import eu.isygoit.service.impl.AuthTypeChangeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authenticated account-security operations. Lives under /private/ on purpose: the JWT filter skips every
 * /public/ path, so an authentication check cannot work there.
 */
@Slf4j
@Validated
@RestController
@InjectExceptionHandler(ImsExceptionHandler.class)
@RequestMapping(path = "/api/v1/private/account-security")
public class AccountSecurityController extends ControllerUtils implements AuthTypeServiceApi {

    @Autowired
    private AuthTypeChangeService authTypeChangeService;

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Boolean> changeAuthType(ChangeAuthTypeRequestDto request) {
        try {
            RequestContextDto context = requestContextService().getCurrentContext();
            if (context == null || isBlank(context.getSenderTenant()) || isBlank(context.getSenderUser())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            return switch (authTypeChangeService.changeAuthType(context.getSenderTenant(), context.getSenderUser(), request)) {
                case CHANGED -> ResponseFactory.responseOk(true);
                case NOT_ALLOWED -> ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                case REAUTH_FAILED -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                case NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND).build();
                case INVALID_AUTH_TYPE -> ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            };
        } catch (Exception e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

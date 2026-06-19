package eu.isygoit.controller;

import eu.isygoit.annotation.InjectExceptionHandler;
import eu.isygoit.api.PasswordServiceApi;
import eu.isygoit.com.rest.controller.ResponseFactory;
import eu.isygoit.com.rest.controller.constants.CtrlConstants;
import eu.isygoit.com.rest.controller.impl.ControllerExceptionHandler;
import eu.isygoit.dto.common.ResetPwdViaTokenRequestDto;
import eu.isygoit.dto.request.*;
import eu.isygoit.dto.response.AccessKeyResponseDto;
import eu.isygoit.dto.response.AccessTokenResponseDto;
import eu.isygoit.enums.IEnumAuth;
import eu.isygoit.enums.IEnumPasswordStatus;
import eu.isygoit.enums.IEnumToken;
import eu.isygoit.enums.IEnumWebToken;
import eu.isygoit.exception.AccountAuthenticationException;
import eu.isygoit.exception.handler.KmsExceptionHandler;
import eu.isygoit.mapper.AccountMapper;
import eu.isygoit.service.*;
import eu.isygoit.service.impl.TokenService;
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

import java.util.Set;

/**
 * The type Password controller.
 */
@Slf4j
@Validated
@Tag(name = "Password", description = "Endpoints for managing passwords")
@RestController
@InjectExceptionHandler(KmsExceptionHandler.class)
@RequestMapping(path = "/api/v1/private/password")
public class PasswordController extends ControllerExceptionHandler implements PasswordServiceApi {

    @Autowired
    private IAccountService accountService;
    @Autowired
    private ITenantService tenantService;
    @Autowired
    private AccountMapper accountMapper;
    @Autowired
    private IPasswordService passwordService;
    @Autowired
    private TokenService tokenService;
    @Autowired
    private RequestContextService requestContextService;

    @Override
    public ResponseEntity<Integer> generateToken(
            GeneratePwdRequestDto generatePwdRequest) {
        log.info("Call generate password for tenant {}", generatePwdRequest);
        try {
            AccessKeyResponseDto accessKeyResponse = passwordService.generateRandomPassword(
                    generatePwdRequest.getTenant()
                    , generatePwdRequest.getTenantUrl()
                    , generatePwdRequest.getEmail()
                    , generatePwdRequest.getUserName()
                    , generatePwdRequest.getFullName()
                    , IEnumAuth.Types.TOKEN);
            //Never return the password
            log.info("password generated for {}/{} : {}", generatePwdRequest.getTenant(), generatePwdRequest.getUserName(), accessKeyResponse.getKey());
            return ResponseFactory.responseOk(accessKeyResponse.getLength());
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<Integer> generatePwd(
            GeneratePwdRequestDto generatePwdRequest) {
        log.info("Call generate password for tenant {}", generatePwdRequest);
        try {
            AccessKeyResponseDto accessKeyResponse = passwordService.generateRandomPassword(
                    generatePwdRequest.getTenant()
                    , generatePwdRequest.getTenantUrl()
                    , generatePwdRequest.getEmail()
                    , generatePwdRequest.getUserName()
                    , generatePwdRequest.getFullName()
                    , IEnumAuth.Types.PWD);
            //Never return the password
            log.info("password generated for {}/{} : {}", generatePwdRequest.getTenant(), generatePwdRequest.getUserName(), accessKeyResponse.getKey());
            return ResponseFactory.responseOk(accessKeyResponse.getLength());
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<String> resetPasswordViaToken(
            ResetPwdViaTokenRequestDto resetPwdViaTokenRequestDto) {
        try {
            passwordService.resetPasswordViaToken(resetPwdViaTokenRequestDto);
            return ResponseFactory.responseOk("password changed successfully");
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<String> changePassword(
            String oldPassword,
            String newPassword) {
        try {
            passwordService.changePassword(requestContextService.getCurrentContext().getSenderTenant(),
                    requestContextService.getCurrentContext().getSenderUser(),
                    oldPassword, newPassword);
            return ResponseFactory.responseOk("password changed successfully");
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<Boolean> patternCheck(
            CheckPwdRequestDto checkPwdRequest) {
        log.info("Call check password for tenant {}", checkPwdRequest);
        try {
            return ResponseFactory.responseOk(passwordService.checkForPattern(checkPwdRequest.getTenant()
                    , checkPwdRequest.getPassword()));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<AccessTokenResponseDto> getAccess(
            AccessRequestDto accessRequest) {
        log.info("Call access for tenant {}", accessRequest);
        try {
            if (!tenantService.isEnabled(accessRequest.getTenant().trim().toLowerCase())) {
                throw new AccountAuthenticationException("tenant disabled: " + accessRequest.getTenant());
            }

            if (IEnumAuth.Types.TOKEN == accessRequest.getAuthType()) {
                try {
                    tokenService.isTokenValid(accessRequest.getTenant(),
                            Set.of(accessRequest.getApplication()),
                            IEnumToken.Types.ACCESS,
                            accessRequest.getPassword(),
                            new StringBuilder(accessRequest.getUserName().trim().toLowerCase())
                                    .append("@")
                                    .append(accessRequest.getTenant().trim().toLowerCase())
                                    .toString());
                } catch (Exception e) {
                    return ResponseFactory.responseOk(AccessTokenResponseDto.builder()
                            .status(IEnumPasswordStatus.Types.UNAUTHORIZED)
                            .build());
                }

                return ResponseFactory.responseOk(AccessTokenResponseDto.builder()
                        .status(IEnumPasswordStatus.Types.VALID)
                        .tokenType(IEnumWebToken.Types.Bearer)
                        .accessToken(tokenService.buildAccessToken(accessRequest.getTenant().trim().toLowerCase(),
                                        Set.of(accessRequest.getApplication()),
                                        accessRequest.getUserName().trim().toLowerCase(),
                                        accessRequest.getIsAdmin())
                                .getToken())
                        .refreshToken(tokenService.buildRefreshToken(accessRequest.getTenant().trim().toLowerCase(),
                                        Set.of(accessRequest.getApplication()),
                                        accessRequest.getUserName().trim().toLowerCase())
                                .getToken())
                        .authorityToken(tokenService.buildAuthorityToken(accessRequest.getTenant().trim().toLowerCase(),
                                        Set.of(accessRequest.getApplication()),
                                        accessRequest.getUserName().trim().toLowerCase(),
                                        accessRequest.getAuthorities())
                                .getToken())
                        .build());
            } else {
                return ResponseFactory.responseOk(AccessTokenResponseDto.builder()
                        .status(passwordService.matches(accessRequest.getTenant().trim().toLowerCase()
                                , accessRequest.getUserName().trim().toLowerCase()
                                , accessRequest.getPassword()
                                , accessRequest.getAuthType()))
                        .tokenType(IEnumWebToken.Types.Bearer)
                        .accessToken(tokenService.buildAccessToken(accessRequest.getTenant().trim().toLowerCase(),
                                Set.of(accessRequest.getApplication()),
                                accessRequest.getUserName().trim().toLowerCase(),
                                accessRequest.getIsAdmin()).getToken())
                        .refreshToken(tokenService.buildRefreshToken(accessRequest.getTenant().trim().toLowerCase(),
                                Set.of(accessRequest.getApplication()),
                                accessRequest.getUserName().trim().toLowerCase()).getToken())
                        .authorityToken(tokenService.buildAuthorityToken(accessRequest.getTenant().trim().toLowerCase(),
                                Set.of(accessRequest.getApplication()),
                                accessRequest.getUserName().trim().toLowerCase(),
                                accessRequest.getAuthorities()).getToken())
                        .build());
            }
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<IEnumPasswordStatus.Types> matches(
            MatchesRequestDto matchesRequest) {
        log.info("Call match password for tenant {}", matchesRequest);
        try {
            return ResponseFactory.responseOk(passwordService.matches(matchesRequest.getTenant()
                    , matchesRequest.getUserName()
                    , matchesRequest.getPassword()
                    , matchesRequest.getAuthType()));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<Boolean> isPwdExpired(
            IsPwdExpiredRequestDto isPwdExpiredRequestDto) {
        log.info("Call isPasswordExpired {}", isPwdExpiredRequestDto);
        try {
            return ResponseFactory.responseOk(passwordService.isExpired(isPwdExpiredRequestDto.getTenant().trim().toLowerCase()
                    , isPwdExpiredRequestDto.getEmail().trim().toLowerCase()
                    , isPwdExpiredRequestDto.getUserName().trim().toLowerCase()
                    , isPwdExpiredRequestDto.getAuthType()));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<Boolean> updateAccount(
            UpdateAccountRequestDto account) {
        log.info("Call update account " + account.toString());
        try {
            return ResponseFactory.responseOk(accountService.checkIfExists(accountMapper.dtoToEntity(account),
                    true));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }
}

package eu.isygoit.controller;

import eu.isygoit.annotation.InjectExceptionHandler;
import eu.isygoit.api.PublicPasswordServiceApi;
import eu.isygoit.com.rest.controller.ResponseFactory;
import eu.isygoit.com.rest.controller.constants.CtrlConstants;
import eu.isygoit.com.rest.controller.impl.ControllerExceptionHandler;
import eu.isygoit.com.rest.controller.impl.ControllerUtils;
import eu.isygoit.dto.common.UserContextRequestDto;
import eu.isygoit.dto.request.AccessRequestDto;
import eu.isygoit.dto.request.GeneratePwdRequestDto;
import eu.isygoit.dto.request.IsPwdExpiredRequestDto;
import eu.isygoit.dto.request.MatchesRequestDto;
import eu.isygoit.dto.response.AccessKeyResponseDto;
import eu.isygoit.dto.response.AccessTokenResponseDto;
import eu.isygoit.enums.IEnumAuth;
import eu.isygoit.enums.IEnumPasswordStatus;
import eu.isygoit.enums.IEnumToken;
import eu.isygoit.enums.IEnumWebToken;
import eu.isygoit.exception.AccountAuthenticationException;
import eu.isygoit.exception.handler.KmsExceptionHandler;
import eu.isygoit.service.IPasswordService;
import eu.isygoit.service.ITenantService;
import eu.isygoit.service.ITokenBuilderService;
import eu.isygoit.service.RequestContextService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

/**
 * The type Public password controller.
 */
@Slf4j
@Validated
@Tag(name = "Public Password", description = "Endpoints for public password operations")
@RestController
@InjectExceptionHandler(KmsExceptionHandler.class)
@RequestMapping(path = "/api/v1/public/password")
public class PublicPasswordController extends ControllerUtils implements PublicPasswordServiceApi {

    @Autowired
    private ITokenBuilderService tokenService;

    @Autowired
    private IPasswordService passwordService;

    @Autowired
    private ITenantService tenantService;

    

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
    public ResponseEntity<Boolean> isOtpExpired(
            IsPwdExpiredRequestDto isPwdExpiredRequestDto) {
        log.info("Call isPasswordExpired {}", isPwdExpiredRequestDto);
        try {
            return ResponseFactory.responseOk(passwordService.isExpired(isPwdExpiredRequestDto.getTenant().trim().toLowerCase()
                    , isPwdExpiredRequestDto.getEmail().trim().toLowerCase()
                    , isPwdExpiredRequestDto.getUserName().trim().toLowerCase()
                    , IEnumAuth.Types.OTP));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<Boolean> isQrcExpired(
            IsPwdExpiredRequestDto isPwdExpiredRequestDto) {
        log.info("Call isPasswordExpired {}", isPwdExpiredRequestDto);
        try {
            return ResponseFactory.responseOk(passwordService.isExpired(isPwdExpiredRequestDto.getTenant().trim().toLowerCase()
                    , isPwdExpiredRequestDto.getEmail().trim().toLowerCase()
                    , isPwdExpiredRequestDto.getUserName().trim().toLowerCase()
                    , IEnumAuth.Types.QRC));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<IEnumPasswordStatus.Types> matchesOtp(
            MatchesRequestDto matchesRequest) {
        log.info("Call match password for tenant {}", matchesRequest);
        try {
            return ResponseFactory.responseOk(passwordService.matches(matchesRequest.getTenant()
                    , matchesRequest.getUserName()
                    , matchesRequest.getPassword()
                    , IEnumAuth.Types.OTP));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<IEnumPasswordStatus.Types> matchesQrc(
            MatchesRequestDto matchesRequest) {
        log.info("Call match password for tenant {}", matchesRequest);
        try {
            return ResponseFactory.responseOk(passwordService.matches(matchesRequest.getTenant()
                    , matchesRequest.getUserName()
                    , matchesRequest.getPassword()
                    , IEnumAuth.Types.QRC));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }


    @Override
    public ResponseEntity<Integer> generateOtp(
            GeneratePwdRequestDto generatePwdRequest) {
        log.info("Call generate password for tenant {}", generatePwdRequest);
        try {
            AccessKeyResponseDto accessKeyResponse = passwordService.generateRandomPassword(
                    requestContextService().getCurrentContext().getSenderTenant(),
                    generatePwdRequest.getTenant(),
                    generatePwdRequest.getTenantUrl(),
                    generatePwdRequest.getEmail(),
                    generatePwdRequest.getUserName(),
                    generatePwdRequest.getFullName(),
                    IEnumAuth.Types.OTP);
            //Never return the password
            log.info("password generated for {}/{} : {}", generatePwdRequest.getTenant(), generatePwdRequest.getUserName(), accessKeyResponse.getKey());
            return ResponseFactory.responseOk(accessKeyResponse.getLength());
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<Integer> generateQrc(
            GeneratePwdRequestDto generatePwdRequest) {
        log.info("Call generate password for tenant {}", generatePwdRequest);
        try {
            AccessKeyResponseDto accessKeyResponse = passwordService.generateRandomPassword(
                    requestContextService().getCurrentContext().getSenderTenant(),
                    generatePwdRequest.getTenant(),
                    generatePwdRequest.getTenantUrl(),
                    generatePwdRequest.getEmail(),
                    generatePwdRequest.getUserName(),
                    generatePwdRequest.getFullName(),
                    IEnumAuth.Types.QRC);
            //Never return the password
            log.info("password generated for {}/{} : {}", generatePwdRequest.getTenant(), generatePwdRequest.getUserName(), accessKeyResponse.getKey());
            return ResponseFactory.responseOk(accessKeyResponse.getLength());
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<Boolean> generateForgotPasswordAccessToken(UserContextRequestDto userContextDto) {
        log.info("Call generateForgotPasswordAccessToken " + userContextDto.toString());
        try {
            tokenService.buildForgotPasswordAccessToken(userContextDto.getTenant(),
                    Set.of(userContextDto.getApplication()),
                    userContextDto.getUserName());
            return ResponseFactory.responseOk(true);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }
}

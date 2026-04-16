package eu.isygoit.controller;

import eu.isygoit.annotation.InjectExceptionHandler;
import eu.isygoit.api.PasswordControllerApi;
import eu.isygoit.com.rest.controller.ResponseFactory;
import eu.isygoit.com.rest.controller.constants.CtrlConstants;
import eu.isygoit.com.rest.controller.impl.ControllerExceptionHandler;
import eu.isygoit.dto.common.ContextRequestDto;
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
import eu.isygoit.jwt.IJwtService;
import eu.isygoit.mapper.AccountMapper;
import eu.isygoit.model.TokenConfig;
import eu.isygoit.service.*;
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
 * The type Password controller.
 */
@Slf4j
@Validated
@Tag(name = "Password", description = "Endpoints for managing passwords")
@RestController
@InjectExceptionHandler(KmsExceptionHandler.class)
@RequestMapping(path = "/api/v1/private/password")
public class PasswordController extends ControllerExceptionHandler implements PasswordControllerApi {

    @Autowired
    private IAccountService accountService;
    @Autowired
    private ITenantService tenantService;
    @Autowired
    private AccountMapper accountMapper;
    @Autowired
    private IPasswordService passwordService;
    @Autowired
    private ITokenService tokenService;
    @Autowired
    private IJwtService jwtService;
    @Autowired
    private ITokenConfigService tokenConfigService;

    @Operation(summary = "Generate password Api",
            description = "Generate password")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = Integer.class))})
    })
    @Override
    public ResponseEntity<Integer> generate(ContextRequestDto requestContext,
                                            IEnumAuth.Types authType,
                                            GeneratePwdRequestDto generatePwdRequest) {
        log.info("Call generate password for tenant {}", generatePwdRequest);
        try {
            AccessKeyResponseDto accessKeyResponse = passwordService.generateRandomPassword(
                    generatePwdRequest.getTenant()
                    , generatePwdRequest.getTenantUrl()
                    , generatePwdRequest.getEmail()
                    , generatePwdRequest.getUserName()
                    , generatePwdRequest.getFullName()
                    , authType);
            //Never return the password
            log.info("password generated for {}/{} : {}", generatePwdRequest.getTenant(), generatePwdRequest.getUserName(), accessKeyResponse.getKey());
            return ResponseFactory.responseOk(accessKeyResponse.getLength());
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Operation(summary = "Reset password via token Api",
            description = "Reset password via token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = String.class))})
    })
    @Override
    public ResponseEntity<String> resetPasswordViaToken(ContextRequestDto requestContext,
                                                        ResetPwdViaTokenRequestDto resetPwdViaTokenRequestDto) {
        try {
            passwordService.resetPasswordViaToken(resetPwdViaTokenRequestDto);
            return ResponseFactory.responseOk("password changed successfully");
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Operation(summary = "Change password Api",
            description = "Change password")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = String.class))})
    })
    @Override
    public ResponseEntity<String> changePassword(ContextRequestDto requestContext,
                                                 String oldPassword,
                                                 String newPassword) {
        try {
            passwordService.changePassword(requestContext.getSenderTenant(), requestContext.getSenderUser(),
                    oldPassword, newPassword);
            return ResponseFactory.responseOk("password changed successfully");
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Operation(summary = "Pattern check Api",
            description = "Pattern check")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = Boolean.class))})
    })
    @Override
    public ResponseEntity<Boolean> patternCheck(ContextRequestDto requestContext,
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

    @Operation(summary = "Get access Api",
            description = "Get access")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = AccessTokenResponseDto.class))})
    })
    @Override
    public ResponseEntity<AccessTokenResponseDto> getAccess(ContextRequestDto requestContext,
                                                            AccessRequestDto accessRequest) {
        log.info("Call access for tenant {}", accessRequest);
        try {
            if (!tenantService.isEnabled(accessRequest.getTenant().trim().toLowerCase())) {
                throw new AccountAuthenticationException("tenant disabled: " + accessRequest.getTenant());
            }

            if (IEnumAuth.Types.TOKEN == accessRequest.getAuthType()) {
                try {
                    TokenConfig tokenConfig = tokenConfigService.buildTokenConfig(accessRequest.getTenant().trim().toLowerCase(),
                            IEnumToken.Types.ACCESS);
                    jwtService.validateToken(accessRequest.getPassword(),
                            new StringBuilder(accessRequest.getUserName().trim().toLowerCase())
                                    .append("@")
                                    .append(accessRequest.getTenant().trim().toLowerCase())
                                    .toString(),
                            tokenConfig.getSecretKey());
                } catch (Exception e) {
                    return ResponseFactory.responseOk(AccessTokenResponseDto.builder()
                            .status(IEnumPasswordStatus.Types.UNAUTHORIZED)
                            .build());
                }

                return ResponseFactory.responseOk(AccessTokenResponseDto.builder()
                        .status(IEnumPasswordStatus.Types.VALID)
                        .tokenType(IEnumWebToken.Types.Bearer)
                        .accessToken(tokenService.createAccessToken(accessRequest.getTenant().trim().toLowerCase(),
                                        accessRequest.getApplication(),
                                        accessRequest.getUserName().trim().toLowerCase(),
                                        accessRequest.getIsAdmin())
                                .getToken())
                        .refreshToken(tokenService.createRefreshToken(accessRequest.getTenant().trim().toLowerCase(),
                                        accessRequest.getApplication(),
                                        accessRequest.getUserName().trim().toLowerCase())
                                .getToken())
                        .authorityToken(tokenService.createAuthorityToken(accessRequest.getTenant().trim().toLowerCase(),
                                        accessRequest.getApplication(),
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
                        .accessToken(tokenService.createAccessToken(accessRequest.getTenant().trim().toLowerCase(),
                                accessRequest.getApplication(),
                                accessRequest.getUserName().trim().toLowerCase(),
                                accessRequest.getIsAdmin()).getToken())
                        .refreshToken(tokenService.createRefreshToken(accessRequest.getTenant().trim().toLowerCase(),
                                accessRequest.getApplication(),
                                accessRequest.getUserName().trim().toLowerCase()).getToken())
                        .authorityToken(tokenService.createAuthorityToken(accessRequest.getTenant().trim().toLowerCase(),
                                accessRequest.getApplication(),
                                accessRequest.getUserName().trim().toLowerCase(),
                                accessRequest.getAuthorities()).getToken())
                        .build());
            }
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Operation(summary = "Matches Api",
            description = "Matches")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = IEnumPasswordStatus.Types.class))})
    })
    @Override
    public ResponseEntity<IEnumPasswordStatus.Types> matches(ContextRequestDto requestContext,
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

    @Operation(summary = "Is password expired Api",
            description = "Is password expired")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = Boolean.class))})
    })
    @Override
    public ResponseEntity<Boolean> isPasswordExpired(ContextRequestDto requestContext,
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

    @Operation(summary = "Update account Api",
            description = "Update account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = Boolean.class))})
    })
    @Override
    public ResponseEntity<Boolean> updateAccount(ContextRequestDto requestContext,
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

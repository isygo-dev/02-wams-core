package eu.isygoit.controller;

import eu.isygoit.annotation.CtrlHandler;
import eu.isygoit.api.PasswordControllerApi;
import eu.isygoit.com.rest.controller.ResponseFactory;
import eu.isygoit.com.rest.controller.constants.CtrlConstants;
import eu.isygoit.com.rest.controller.impl.ControllerExceptionHandler;
import eu.isygoit.dto.common.RequestContextDto;
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
@RestController
@CtrlHandler(KmsExceptionHandler.class)
@RequestMapping(path = "/api/v1/private/password")
public class PasswordController extends ControllerExceptionHandler implements PasswordControllerApi {

    @Autowired
    private IAccountService accountService;
    @Autowired
    private IDomainService domainService;
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

    @Override
    public ResponseEntity<Integer> generate(//RequestContextDto requestContext,
                                            IEnumAuth.Types authType,
                                            GeneratePwdRequestDto generatePwdRequest) {
        log.info("Call generate password for domain {}", generatePwdRequest);
        try {
            AccessKeyResponseDto accessKeyResponse = passwordService.generateRandomPassword(
                    generatePwdRequest.getDomain()
                    , generatePwdRequest.getDomainUrl()
                    , generatePwdRequest.getEmail()
                    , generatePwdRequest.getUserName()
                    , generatePwdRequest.getFullName()
                    , authType);
            //Never return the password
            log.info("password generated for {}/{} : {}", generatePwdRequest.getDomain(), generatePwdRequest.getUserName(), accessKeyResponse.getKey());
            return ResponseFactory.responseOk(accessKeyResponse.getLength());
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<String> resetPasswordViaToken(//RequestContextDto requestContext,
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
    public ResponseEntity<String> changePassword(RequestContextDto requestContext,
                                                 String oldPassword,
                                                 String newPassword) {
        try {
            passwordService.changePassword(requestContext.getSenderDomain(), requestContext.getSenderUser(),
                    oldPassword, newPassword);
            return ResponseFactory.responseOk("password changed successfully");
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<Boolean> patternCheck(//RequestContextDto requestContext,
                                                CheckPwdRequestDto checkPwdRequest) {
        log.info("Call check password for domain {}", checkPwdRequest);
        try {
            return ResponseFactory.responseOk(passwordService.checkForPattern(checkPwdRequest.getDomain()
                    , checkPwdRequest.getPassword()));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<AccessTokenResponseDto> getAccess(//RequestContextDto requestContext,
                                                            AccessRequestDto accessRequest) {
        log.info("Call access for domain {}", accessRequest);
        try {
            if (!domainService.isEnabled(accessRequest.getDomain().trim().toLowerCase())) {
                throw new AccountAuthenticationException("domain disabled: " + accessRequest.getDomain());
            }

            if (IEnumAuth.Types.TOKEN == accessRequest.getAuthType()) {
                try {
                    TokenConfig tokenConfig = tokenConfigService.buildTokenConfig(accessRequest.getDomain().trim().toLowerCase(),
                            IEnumToken.Types.ACCESS);
                    jwtService.validateToken(accessRequest.getPassword(),
                            new StringBuilder(accessRequest.getUserName().trim().toLowerCase())
                                    .append("@")
                                    .append(accessRequest.getDomain().trim().toLowerCase())
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
                        .accessToken(tokenService.createAccessToken(accessRequest.getDomain().trim().toLowerCase(),
                                        accessRequest.getApplication(),
                                        accessRequest.getUserName().trim().toLowerCase(),
                                        accessRequest.getIsAdmin())
                                .getToken())
                        .refreshToken(tokenService.createRefreshToken(accessRequest.getDomain().trim().toLowerCase(),
                                        accessRequest.getApplication(),
                                        accessRequest.getUserName().trim().toLowerCase())
                                .getToken())
                        .authorityToken(tokenService.createAuthorityToken(accessRequest.getDomain().trim().toLowerCase(),
                                        accessRequest.getApplication(),
                                        accessRequest.getUserName().trim().toLowerCase(),
                                        accessRequest.getAuthorities())
                                .getToken())
                        .build());
            } else {
                return ResponseFactory.responseOk(AccessTokenResponseDto.builder()
                        .status(passwordService.matches(accessRequest.getDomain().trim().toLowerCase()
                                , accessRequest.getUserName().trim().toLowerCase()
                                , accessRequest.getPassword()
                                , accessRequest.getAuthType()))
                        .tokenType(IEnumWebToken.Types.Bearer)
                        .accessToken(tokenService.createAccessToken(accessRequest.getDomain().trim().toLowerCase(),
                                accessRequest.getApplication(),
                                accessRequest.getUserName().trim().toLowerCase(),
                                accessRequest.getIsAdmin()).getToken())
                        .refreshToken(tokenService.createRefreshToken(accessRequest.getDomain().trim().toLowerCase(),
                                accessRequest.getApplication(),
                                accessRequest.getUserName().trim().toLowerCase()).getToken())
                        .authorityToken(tokenService.createAuthorityToken(accessRequest.getDomain().trim().toLowerCase(),
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

    @Override
    public ResponseEntity<IEnumPasswordStatus.Types> matches(//RequestContextDto requestContext,
                                                             MatchesRequestDto matchesRequest) {
        log.info("Call match password for domain {}", matchesRequest);
        try {
            return ResponseFactory.responseOk(passwordService.matches(matchesRequest.getDomain()
                    , matchesRequest.getUserName()
                    , matchesRequest.getPassword()
                    , matchesRequest.getAuthType()));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<Boolean> isPasswordExpired(//RequestContextDto requestContext,
                                                     IsPwdExpiredRequestDto isPwdExpiredRequestDto) {
        log.info("Call isPasswordExpired {}", isPwdExpiredRequestDto);
        try {
            return ResponseFactory.responseOk(passwordService.isExpired(isPwdExpiredRequestDto.getDomain().trim().toLowerCase()
                    , isPwdExpiredRequestDto.getEmail().trim().toLowerCase()
                    , isPwdExpiredRequestDto.getUserName().trim().toLowerCase()
                    , isPwdExpiredRequestDto.getAuthType()));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<Boolean> updateAccount(//RequestContextDto requestContext,
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

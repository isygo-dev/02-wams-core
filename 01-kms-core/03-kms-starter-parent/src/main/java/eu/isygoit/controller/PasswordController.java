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
import eu.isygoit.enums.IEnumAppToken;
import eu.isygoit.enums.IEnumAuth;
import eu.isygoit.enums.IEnumPasswordStatus;
import eu.isygoit.enums.IEnumWebToken;
import eu.isygoit.exception.AccountAuthenticationException;
import eu.isygoit.exception.handler.KmsExceptionHandler;
import eu.isygoit.jwt.IJwtService;
import eu.isygoit.mapper.AccountMapper;
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

    private final IAccountService accountService;
    private final IDomainService domainService;
    private final AccountMapper accountMapper;
    private final IPasswordService passwordService;
    private final ITokenService tokenService;
    private final IJwtService jwtService;
    private final ITokenConfigService tokenConfigService;

    @Autowired
    public PasswordController(IAccountService accountService, IDomainService domainService, AccountMapper accountMapper, IPasswordService passwordService, ITokenService tokenService, IJwtService jwtService, ITokenConfigService tokenConfigService) {
        this.accountService = accountService;
        this.domainService = domainService;
        this.accountMapper = accountMapper;
        this.passwordService = passwordService;
        this.tokenService = tokenService;
        this.jwtService = jwtService;
        this.tokenConfigService = tokenConfigService;
    }

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
            return ResponseFactory.ResponseOk(accessKeyResponse.getLength());
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
            return ResponseFactory.ResponseOk("password changed successfully");
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
            return ResponseFactory.ResponseOk("password changed successfully");
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
            return ResponseFactory.ResponseOk(passwordService.isPasswordPatternValid(checkPwdRequest.getDomain()
                    , checkPwdRequest.getPassword()));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<AccessTokenResponseDto> getAccess(AccessRequestDto accessRequest) {
        var domain = accessRequest.getDomain().trim().toLowerCase();
        var username = accessRequest.getUserName().trim().toLowerCase();
        log.info("Call access for domain {}", accessRequest);

        try {
            if (!domainService.isEnabled(domain)) {
                throw new AccountAuthenticationException("domain disabled: " + domain);
            }

            // Use a switch statement to handle different authentication types
            return switch (accessRequest.getAuthType()) {
                case TOKEN -> handleTokenAuthentication(accessRequest, domain, username);
                case PWD -> handlePasswordAuthentication(accessRequest, domain, username);
                default -> handlePasswordAuthentication(accessRequest, domain, username);
            };

        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    private ResponseEntity<AccessTokenResponseDto> handleTokenAuthentication(AccessRequestDto accessRequest, String domain, String username) {
        try {
            var tokenConfig = tokenConfigService.buildTokenConfig(domain, IEnumAppToken.Types.ACCESS)
                    .orElseThrow(() -> new AccountAuthenticationException("Token config not found for domain: " + domain));

            jwtService.validateToken(accessRequest.getPassword(), username + "@" + domain, tokenConfig.getSecretKey());

            return buildAccessTokenResponse(accessRequest, domain, username, IEnumPasswordStatus.Types.VALID);
        } catch (Exception e) {
            return ResponseFactory.ResponseOk(AccessTokenResponseDto.builder()
                    .status(IEnumPasswordStatus.Types.UNAUTHORIZED)
                    .build());
        }
    }

    private ResponseEntity<AccessTokenResponseDto> handlePasswordAuthentication(AccessRequestDto accessRequest, String domain, String username) {
        var status = passwordService.matches(domain, username, accessRequest.getPassword(), accessRequest.getAuthType());
        return buildAccessTokenResponse(accessRequest, domain, username, status);
    }

    private ResponseEntity<AccessTokenResponseDto> buildAccessTokenResponse(AccessRequestDto accessRequest, String domain, String username, IEnumPasswordStatus.Types status) {
        var accessToken = tokenService.createAccessToken(domain, accessRequest.getApplication(), username, accessRequest.getIsAdmin()).getToken();
        var refreshToken = tokenService.createRefreshToken(domain, accessRequest.getApplication(), username).getToken();
        var authorityToken = tokenService.createAuthorityToken(domain, accessRequest.getApplication(), username, accessRequest.getAuthorities()).getToken();

        return ResponseFactory.ResponseOk(AccessTokenResponseDto.builder()
                .status(status)
                .tokenType(IEnumWebToken.Types.Bearer)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .authorityToken(authorityToken)
                .build());
    }

    @Override
    public ResponseEntity<IEnumPasswordStatus.Types> matches(//RequestContextDto requestContext,
                                                             MatchesRequestDto matchesRequest) {
        log.info("Call match password for domain {}", matchesRequest);
        try {
            return ResponseFactory.ResponseOk(passwordService.matches(matchesRequest.getDomain()
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
            return ResponseFactory.ResponseOk(passwordService.isExpired(isPwdExpiredRequestDto.getDomain().trim().toLowerCase()
                    , isPwdExpiredRequestDto.getEmail().trim().toLowerCase()
                    , isPwdExpiredRequestDto.getUserName().trim().toLowerCase()
                    , isPwdExpiredRequestDto.getAuthType()));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<UpdateAccountRequestDto> updateAccount(//RequestContextDto requestContext,
                                                                 UpdateAccountRequestDto account) {
        log.info("Call update account " + account.toString());
        try {
            return accountService.checkIfExists(accountMapper.dtoToEntity(account), true)
                    .map(acc -> {
                        return ResponseFactory.ResponseOk(accountMapper.entityToDto(acc));
                    }).orElse(
                            ResponseFactory.ResponseNotFound()
                    );
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }
}

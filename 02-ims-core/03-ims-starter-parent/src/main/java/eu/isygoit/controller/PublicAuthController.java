package eu.isygoit.controller;

import eu.isygoit.annotation.InjectExceptionHandler;
import eu.isygoit.api.PublicAuthControllerApi;
import eu.isygoit.com.rest.controller.ResponseFactory;
import eu.isygoit.com.rest.controller.constants.CtrlConstants;
import eu.isygoit.com.rest.controller.impl.ControllerExceptionHandler;
import eu.isygoit.config.AppProperties;
import eu.isygoit.config.JwtProperties;
import eu.isygoit.dto.common.RequestContextDto;
import eu.isygoit.dto.common.SystemInfoDto;
import eu.isygoit.dto.common.UserContextDto;
import eu.isygoit.dto.data.DomainDto;
import eu.isygoit.dto.data.ThemeDto;
import eu.isygoit.dto.request.AccountAuthTypeRequest;
import eu.isygoit.dto.request.AuthenticationRequestDto;
import eu.isygoit.dto.request.RegisteredUserDto;
import eu.isygoit.dto.request.RequestTrackingDto;
import eu.isygoit.dto.response.AuthResponseDto;
import eu.isygoit.dto.response.UserAccountDto;
import eu.isygoit.dto.response.UserContext;
import eu.isygoit.dto.response.UserDataResponseDto;
import eu.isygoit.enums.IEnumJwtStorage;
import eu.isygoit.enums.IEnumWebToken;
import eu.isygoit.exception.handler.ImsExceptionHandler;
import eu.isygoit.mapper.DomainMapper;
import eu.isygoit.mapper.RegistredUserMapper;
import eu.isygoit.mapper.ThemeMapper;
import eu.isygoit.model.Account;
import eu.isygoit.model.Domain;
import eu.isygoit.remote.kms.KmsPublicPasswordService;
import eu.isygoit.service.IAccountService;
import eu.isygoit.service.IAuthService;
import eu.isygoit.service.IDomainService;
import eu.isygoit.service.IThemeService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The type Public auth controller.
 */
@Slf4j
@Validated
@RestController
@InjectExceptionHandler(ImsExceptionHandler.class)
@RequestMapping(path = "/api/v1/public/user")
public class PublicAuthController extends ControllerExceptionHandler implements PublicAuthControllerApi {

    private final AppProperties appProperties;
    private final JwtProperties jwtProperties;

    /**
     * The Register new account mapper.
     */
    @Autowired
    private RegistredUserMapper registredUserMapper;
    @Autowired
    private KmsPublicPasswordService kmsPublicPasswordService;
    @Autowired
    private IAccountService accountService;
    @Autowired
    private IThemeService themeService;
    @Autowired
    private IAuthService authService;
    @Autowired
    private IDomainService tenantService;
    @Autowired
    private ThemeMapper themeMapper;
    @Autowired
    private DomainMapper tenantMapper;

    /**
     * Instantiates a new Public auth controller.
     *
     * @param appProperties the app properties
     * @param jwtProperties the jwt properties
     */
    public PublicAuthController(AppProperties appProperties, JwtProperties jwtProperties) {
        this.appProperties = appProperties;
        this.jwtProperties = jwtProperties;
    }

    public ResponseEntity<AuthResponseDto> authenticate(HttpServletRequest request, HttpServletResponse response,
                                                        AuthenticationRequestDto authRequestDto) {
        try {
            //Remove left & right spaces
            authRequestDto.setTenant(authRequestDto.getTenant().trim().toLowerCase());
            authRequestDto.setUserName(authRequestDto.getUserName().trim().toLowerCase());
            authRequestDto.setPassword(authRequestDto.getPassword().trim());

            AuthResponseDto authenticate = authService.authenticate(RequestTrackingDto.getFromRequest(request),
                    authRequestDto.getTenant(),
                    authRequestDto.getUserName(),
                    authRequestDto.getApplication(),
                    authRequestDto.getPassword(),
                    authRequestDto.getAuthType());

            if (jwtProperties.getJwtStorageType() == IEnumJwtStorage.Types.COOKIE) {
                response.addCookie(this.createCookie("token_type", IEnumWebToken.Types.Bearer.meaning()));
                response.addCookie(this.createCookie("access_token", authenticate.getAccessToken()));
                response.addCookie(this.createCookie("refresh_token", authenticate.getRefreshToken()));
                return ResponseFactory.responseOk(AuthResponseDto.builder()
                        .build());
            }

            Account account = accountService.findByTenantAndUserName(authRequestDto.getTenant(), authRequestDto.getUserName());
            Domain tenant = tenantService.findByName(authRequestDto.getTenant());
            ThemeDto theme = themeMapper.entityToDto(themeService.findThemeByAccountCodeAndDomainCode(account.getCode(), tenant.getCode()));
            UserDataResponseDto userDataResponseDto = UserDataResponseDto.builder()
                    .id(account.getId())
                    .userName(account.getCode())
                    .firstName(account.getAccountDetails().getFirstName())
                    .lastName(account.getAccountDetails().getLastName())
                    .applications(accountService.buildAllowedTools(account, authenticate.getAccessToken()))
                    .email(account.getEmail())
                    .tenantId(tenant.getId())
                    .tenantImagePath(tenant.getImagePath())
                    .language(account.getLanguage())
                    .role(account.getFunctionRole())
                    .build();

            return ResponseFactory.responseOk(AuthResponseDto.builder()
                    .tokenType(IEnumWebToken.Types.Bearer)
                    .accessToken(authenticate.getAccessToken())
                    .refreshToken(authenticate.getRefreshToken())
                    .authorityToken(authenticate.getAuthorityToken())
                    .userDataResponseDto(userDataResponseDto)
                    .theme(theme)
                    .systemInfo(SystemInfoDto
                            .builder()
                            .name(appProperties.getApplicationName())
                            .version(appProperties.getApplicationVersion())
                            .build())
                    .build());
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<Boolean> generateForgotPWDToken(UserContextDto userContextDto) {
        try {
            kmsPublicPasswordService.generateForgotPasswordAccessToken(userContextDto);
            return ResponseFactory.responseOk(true);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    private Cookie createCookie(String name, String value) {
        Cookie cookie = new Cookie(name, value);
        cookie.setMaxAge(7 * 24 * 60 * 60); // expires in 7 days
        cookie.setSecure(true);
        cookie.setHttpOnly(true);
        cookie.setPath("/"); // Global
        return cookie;
    }

    @Override
    public ResponseEntity<Boolean> registerUser(RegisteredUserDto registeredUserDto) {
        try {
            return ResponseFactory.responseOk(authService.registerUser(registredUserMapper.dtoToEntity(registeredUserDto)));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<DomainDto> getTenantByName(String tenant) {
        log.info("get tenant by name {}", tenant);
        try {
            return ResponseFactory.responseOk(tenantMapper.entityToDto(tenantService.findByName(tenant)));
        } catch (Throwable e) {
            log.error("<Error>: get by name : {} ", e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<UserContext> getAuthenticationType(AccountAuthTypeRequest accountAuthTypeRequest) {
        try {
            return ResponseFactory.responseOk(accountService.getAuthenticationType(accountAuthTypeRequest));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<List<UserAccountDto>> getAvailableEmailAccounts(String email) {
        try {
            return ResponseFactory.responseOk(accountService.getAvailableEmailAccounts(email));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<Boolean> switchAuthType(RequestContextDto requestContext,
                                                  AccountAuthTypeRequest accountAuthTypeRequest) {
        try {
            return ResponseFactory.responseOk(accountService.switchAuthType(requestContext.getSenderTenant(), accountAuthTypeRequest));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }
}

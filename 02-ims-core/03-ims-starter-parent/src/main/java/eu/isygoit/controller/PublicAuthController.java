package eu.isygoit.controller;

import eu.isygoit.annotation.CtrlHandler;
import eu.isygoit.api.PublicAuthControllerApi;
import eu.isygoit.app.ApplicationContextService;
import eu.isygoit.com.rest.controller.ResponseFactory;
import eu.isygoit.com.rest.controller.constants.CtrlConstants;
import eu.isygoit.com.rest.controller.impl.ControllerExceptionHandler;
import eu.isygoit.config.AppProperties;
import eu.isygoit.config.JwtProperties;
import eu.isygoit.dto.common.SystemInfoDto;
import eu.isygoit.dto.common.UserContextDto;
import eu.isygoit.dto.data.DomainDto;
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
import eu.isygoit.exception.AccountNotFoundException;
import eu.isygoit.exception.DomainNotFoundException;
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
import java.util.Optional;

@Slf4j
@Validated
@RestController
@CtrlHandler(ImsExceptionHandler.class)
@RequestMapping(path = "/api/v1/public/user")
public class PublicAuthController extends ControllerExceptionHandler implements PublicAuthControllerApi {

    private final ApplicationContextService applicationContextService;

    @Override
    protected ApplicationContextService getApplicationContextServiceInstance() {
        return applicationContextService;
    }

    private final AppProperties appProperties;
    private final JwtProperties jwtProperties;
    private final RegistredUserMapper registredUserMapper;
    private final KmsPublicPasswordService kmsPublicPasswordService;
    private final IAccountService accountService;
    private final IAuthService authService;
    private final IDomainService domainService;
    private final DomainMapper domainMapper;

    @Autowired
    public PublicAuthController(AppProperties appProperties, JwtProperties jwtProperties, RegistredUserMapper registredUserMapper, KmsPublicPasswordService kmsPublicPasswordService, IAccountService accountService, IThemeService themeService, IAuthService authService, IDomainService domainService, ThemeMapper themeMapper, ApplicationContextService applicationContextService, DomainMapper domainMapper) {
        this.appProperties = appProperties;
        this.jwtProperties = jwtProperties;
        this.registredUserMapper = registredUserMapper;
        this.kmsPublicPasswordService = kmsPublicPasswordService;
        this.accountService = accountService;
        this.authService = authService;
        this.domainService = domainService;
        this.applicationContextService = applicationContextService;
        this.domainMapper = domainMapper;
    }

    /**
     * Authenticates a user based on the given credentials and generates JWT tokens.
     *
     * @param request The HTTP request object.
     * @param response The HTTP response object where the JWT tokens might be stored in cookies.
     * @param authRequestDto The authentication request containing user credentials.
     * @return A response containing authentication results and tokens.
     */
    public ResponseEntity<AuthResponseDto> authenticate(HttpServletRequest request, HttpServletResponse response,
                                                        AuthenticationRequestDto authRequestDto) {
        try {
            // Normalize and trim user input to prevent errors caused by leading/trailing spaces.
            authRequestDto.setDomain(authRequestDto.getDomain().trim().toLowerCase());
            authRequestDto.setUserName(authRequestDto.getUserName().trim().toLowerCase());
            authRequestDto.setPassword(authRequestDto.getPassword().trim());

            log.info("Authenticating user: {} for domain: {}", authRequestDto.getUserName(), authRequestDto.getDomain());

            // Perform the authentication using the provided credentials
            var authenticate = authService.authenticate(RequestTrackingDto.getFromRequest(request),
                    authRequestDto.getDomain(),
                    authRequestDto.getUserName(),
                    authRequestDto.getApplication(),
                    authRequestDto.getPassword(),
                    authRequestDto.getAuthType());

            // Log the successful authentication
            log.info("Authentication successful for user: {}", authRequestDto.getUserName());

            // Handle token storage based on configured storage type (cookie or response body)
            if (jwtProperties.getJwtStorageType() == IEnumJwtStorage.Types.COOKIE) {
                response.addCookie(createCookie("token_type", IEnumWebToken.Types.Bearer.meaning()));
                response.addCookie(createCookie("access_token", authenticate.getAccessToken()));
                response.addCookie(createCookie("refresh_token", authenticate.getRefreshToken()));
                log.info("JWT tokens added to response cookies for user: {}", authRequestDto.getUserName());
                return ResponseFactory.ResponseOk(AuthResponseDto.builder().build());
            }

            // Retrieve account and domain for user data
            var account = accountService.getByDomainAndUserName(authRequestDto.getDomain(), authRequestDto.getUserName())
                    .orElseThrow(() -> new AccountNotFoundException("with domain " + authRequestDto.getDomain()
                            + " and user name " + authRequestDto.getUserName()));

            var domain = domainService.getByName(authRequestDto.getDomain())
                    .orElseThrow(() -> new DomainNotFoundException("with name " + authRequestDto.getDomain()));

            var userDataResponseDto = UserDataResponseDto.builder()
                    .id(account.getId())
                    .userName(account.getCode())
                    .firstName(account.getAccountDetails().getFirstName())
                    .lastName(account.getAccountDetails().getLastName())
                    .applications(accountService.buildAllowedTools(account, authenticate.getAccessToken()))
                    .email(account.getEmail())
                    .domainId(domain.getId())
                    .domainImagePath(domain.getImagePath())
                    .language(account.getLanguage())
                    .role(account.getFunctionRole())
                    .build();

            log.info("User data successfully retrieved for: {}", authRequestDto.getUserName());

            return ResponseFactory.ResponseOk(AuthResponseDto.builder()
                    .tokenType(IEnumWebToken.Types.Bearer)
                    .accessToken(authenticate.getAccessToken())
                    .refreshToken(authenticate.getRefreshToken())
                    .authorityToken(authenticate.getAuthorityToken())
                    .userDataResponseDto(userDataResponseDto)
                    .systemInfo(SystemInfoDto.builder()
                            .name(appProperties.getApplicationName())
                            .version(appProperties.getApplicationVersion())
                            .build())
                    .build());
        } catch (Exception e) {
            // Log detailed exception information for troubleshooting
            log.error("Error during authentication: {}", e.getMessage(), e);
            return getBackExceptionResponse(e);
        }
    }

    /**
     * Generates a forgot password token for the given user.
     *
     * @param userContextDto The user context needed for generating the token.
     * @return A response indicating whether the token generation was successful.
     */
    @Override
    public ResponseEntity<Boolean> generateForgotPWDToken(UserContextDto userContextDto) {
        try {
            log.info("Generating forgot password token for user: {}", userContextDto.getUserName());
            kmsPublicPasswordService.generateForgotPasswordAccessToken(userContextDto);
            return ResponseFactory.ResponseOk(true);
        } catch (Exception e) {
            log.error("Error during forgot password token generation: {}", e.getMessage(), e);
            return getBackExceptionResponse(e);
        }
    }

    /**
     * Creates a cookie with the specified name and value.
     *
     * @param name The name of the cookie.
     * @param value The value to store in the cookie.
     * @return The created Cookie object.
     */
    private Cookie createCookie(String name, String value) {
        var cookie = new Cookie(name, value);
        cookie.setMaxAge(7 * 24 * 60 * 60); // expires in 7 days
        cookie.setSecure(true);
        cookie.setHttpOnly(true);
        cookie.setPath("/"); // Global path for the cookie
        log.info("Created secure cookie: {} for value: {}", name, value);
        return cookie;
    }

    /**
     * Registers a new user based on the provided registration information.
     *
     * @param registeredUserDto The registration details of the user.
     * @return A response indicating whether the registration was successful.
     */
    @Override
    public ResponseEntity<Boolean> registerUser(RegisteredUserDto registeredUserDto) {
        try {
            log.info("Registering user: {}", registeredUserDto.getFirstName() + " " + registeredUserDto.getLastName());
            return ResponseFactory.ResponseOk(authService.registerUser(registredUserMapper.dtoToEntity(registeredUserDto)));
        } catch (Exception e) {
            log.error("Error during user registration: {}", e.getMessage(), e);
            return getBackExceptionResponse(e);
        }
    }

    /**
     * Retrieves the domain information by its name.
     *
     * @param domain The name of the domain to retrieve.
     * @return The domain details.
     */
    @Override
    public ResponseEntity<DomainDto> getDomainByName(String domain) {
        log.info("Fetching domain details for: {}", domain);
        try {
            return ResponseFactory.ResponseOk(domainMapper.entityToDto(domainService.getByName(domain)
                    .orElseThrow(() -> new DomainNotFoundException("with name " + domain))));
        } catch (Exception e) {
            log.error("Error fetching domain by name: {}. Exception: {}", domain, e.getMessage(), e);
            return getBackExceptionResponse(e);
        }
    }

    /**
     * Retrieves the authentication type for the specified account.
     *
     * @param accountAuthTypeRequest The request containing account details for fetching the authentication type.
     * @return The authentication type.
     */
    @Override
    public ResponseEntity<UserContext> getAuthenticationType(AccountAuthTypeRequest accountAuthTypeRequest) {
        try {
            log.info("Fetching authentication type for account: {}", accountAuthTypeRequest.getUserName());
            return ResponseFactory.ResponseOk(accountService.getAuthenticationType(accountAuthTypeRequest));
        } catch (Exception e) {
            log.error("Error fetching authentication type for account: {}", accountAuthTypeRequest.getUserName(), e);
            return getBackExceptionResponse(e);
        }
    }

    /**
     * Retrieves available email accounts for the given email.
     *
     * @param email The email address to check for available accounts.
     * @return A list of available user accounts.
     */
    @Override
    public ResponseEntity<List<UserAccountDto>> getAvailableEmailAccounts(String email) {
        try {
            log.info("Fetching available email accounts for: {}", email);
            return ResponseFactory.ResponseOk(accountService.getAvailableEmailAccounts(email));
        } catch (Exception e) {
            log.error("Error fetching available email accounts for: {}", email, e);
            return getBackExceptionResponse(e);
        }
    }

    /**
     * Switches the authentication type for the specified account.
     *
     * @param accountAuthTypeRequest The request containing the new authentication type.
     * @return A response indicating whether the authentication type switch was successful.
     */
    @Override
    public ResponseEntity<Boolean> switchAuthType(AccountAuthTypeRequest accountAuthTypeRequest) {
        try {
            log.info("Switching authentication type for account: {}", accountAuthTypeRequest.getUserName());
            return ResponseFactory.ResponseOk(accountService.switchAuthType(accountAuthTypeRequest));
        } catch (Exception e) {
            log.error("Error switching authentication type for account: {}", accountAuthTypeRequest.getUserName(), e);
            return getBackExceptionResponse(e);
        }
    }
}
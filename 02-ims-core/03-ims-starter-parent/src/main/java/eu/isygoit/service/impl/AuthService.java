package eu.isygoit.service.impl;

import eu.isygoit.constants.AppParameterConstants;
import eu.isygoit.dto.request.AccessRequestDto;
import eu.isygoit.dto.request.RequestTrackingDto;
import eu.isygoit.dto.response.AccessTokenResponseDto;
import eu.isygoit.dto.response.AuthResponseDto;
import eu.isygoit.enums.IEnumAuth;
import eu.isygoit.exception.*;
import eu.isygoit.model.ConnectionTracking;
import eu.isygoit.model.RegistredUser;
import eu.isygoit.remote.kms.KmsPasswordService;
import eu.isygoit.repository.RegistredUserRepository;
import eu.isygoit.security.CustomAuthentification;
import eu.isygoit.security.CustomUserDetails;
import eu.isygoit.service.IAccountService;
import eu.isygoit.service.IAppParameterService;
import eu.isygoit.service.IAuthService;
import eu.isygoit.service.IDomainService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class AuthService implements IAuthService {

    private final KmsPasswordService kmsPasswordService;
    private final AuthenticationManager authenticationManager;
    private final IAccountService accountService;
    private final IDomainService domainService;
    private final IAppParameterService parameterService;
    private final RegistredUserRepository registredUserRepository;

    public AuthService(KmsPasswordService kmsPasswordService,
                       AuthenticationManager authenticationManager,
                       IAccountService accountService,
                       IDomainService domainService,
                       IAppParameterService parameterService,
                       RegistredUserRepository registredUserRepository) {
        this.kmsPasswordService = kmsPasswordService;
        this.authenticationManager = authenticationManager;
        this.accountService = accountService;
        this.domainService = domainService;
        this.parameterService = parameterService;
        this.registredUserRepository = registredUserRepository;
    }

    @Override
    public AuthResponseDto authenticate(RequestTrackingDto requestTracking, String domain, String userName, String application, String password, IEnumAuth.Types authType) {
        String userDomain = domain + "/" + userName;

        if (!domainService.isEnabled(domain)) {
            log.error("Authentication attempt for disabled domain: {}", userDomain);
            throw new AccountAuthenticationException("Domain disabled: " + userDomain);
        }

        log.info("Initiating authentication for user: {} in domain: {} to access application: {}", userName, domain, application);

        // Use Optional to handle parameter retrieval more elegantly
        Optional<String> checkAppAllowed = Optional.ofNullable(parameterService.getValueByDomainAndName(domain, AppParameterConstants.IS_APP_ALLOWED, true, AppParameterConstants.NO));

        // Using stream to check if the application is allowed
        checkAppAllowed.filter(AppParameterConstants.YES::equals)
                .filter(appAllowed -> !accountService.isApplicationAllowed(domain, userName, application))
                .ifPresent(appAllowed -> {
                    log.warn("Application {} is not allowed for user {}", application, userDomain);
                    throw new ApplicationNotAllowedException("Application not allowed for user: " + userDomain);
                });

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new CustomAuthentification(userDomain + "@" + authType, password, List.of())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.info("Authentication successful for user: {}", userDomain);

            return getAccessTokenResponse(authentication, requestTracking, domain, userName, application, password, authType);

        } catch (AuthenticationException e) {
            log.error("Authentication failed for user: {}", userDomain, e);
            throw new AccountAuthenticationException("Authentication failed for user: " + userDomain);
        }
    }

    private AuthResponseDto getAccessTokenResponse(Authentication authentication, RequestTrackingDto requestTracking, String domain, String userName, String application, String password, IEnumAuth.Types authType) {
        try {
            // Making a remote call to KMS to get access details
            var result = kmsPasswordService.getAccess(
                    AccessRequestDto.builder()
                            .domain(domain)
                            .userName(userName)
                            .application(application)
                            .isAdmin(((CustomUserDetails) authentication.getPrincipal()).getIsAdmin())
                            .password(password)
                            .authType(authType)
                            .authorities(((CustomUserDetails) authentication.getPrincipal()).getAuthorities().stream()
                                    .map(GrantedAuthority::getAuthority)
                                    .collect(Collectors.toList()))  // using stream for authorities
                            .build()
            );

            // Use Optional to handle response and its status check
            Optional.ofNullable(result.getBody())
                    .filter(body -> result.getStatusCode().is2xxSuccessful())
                    .ifPresentOrElse(accessResponse -> {
                        switch (accessResponse.getStatus()) {
                            case VALID -> {
                                log.info("Access granted for user: {}", domain + "/" + userName);
                                requestTracking.setAppOrigin(application);
                                trackUserConnections(domain, userName, application, requestTracking);
                            }
                            case LOCKED -> {
                                log.warn("Password is locked for user: {}", domain + "/" + userName);
                                throw new LockedPasswordException("Locked password for user: " + domain + "/" + userName);
                            }
                            case EXPIRED -> {
                                log.warn("Password has expired for user: {}", domain + "/" + userName);
                                throw new ExpiredPasswordException("Expired password for user: " + domain + "/" + userName);
                            }
                            case DEPRECATED, BROKEN, BAD -> {
                                log.error("Password is deprecated or broken for user: {}", domain + "/" + userName);
                                throw new DeprecatedPasswordException("Deprecated password for user: " + domain + "/" + userName);
                            }
                            default -> {
                                log.error("Unauthorized access attempt for user: {}", domain + "/" + userName);
                                throw new UnauthorizedException("Unauthorized access for user: " + domain + "/" + userName);
                            }
                        }
                    }, () -> {
                        log.error("Remote service returned unsuccessful response for user: {}", domain + "/" + userName);
                        throw new UnauthorizedException("Unauthorized access for user: " + domain + "/" + userName);
                    });

            // Return the AuthResponseDto, only if the response was successful
            return result.getBody() != null ? buildAuthResponseDto(result.getBody()) : null;

        } catch (Exception e) {
            log.error("Remote call to KMS service failed for user: {}", domain + "/" + userName, e);
            throw new RemoteCallFailedException(e);
        }
    }

    private void trackUserConnections(String domain, String userName, String application, RequestTrackingDto requestTracking) {
        log.info("Tracking user connection for user: {} accessing application: {}", domain + "/" + userName, application);
        accountService.trackUserConnections(domain, userName, ConnectionTracking.builder()
                .device(requestTracking.getDevice())
                .browser(requestTracking.getBrowser())
                .ipAddress(requestTracking.getIpOrigin())
                .logApp(application)
                .loginDate(new Date())
                .build());
    }

    private AuthResponseDto buildAuthResponseDto(AccessTokenResponseDto accessResponse) {
        return AuthResponseDto.builder()
                .tokenType(accessResponse.getTokenType())
                .accessToken(accessResponse.getAccessToken())
                .refreshToken(accessResponse.getRefreshToken())
                .authorityToken(accessResponse.getAuthorityToken())
                .build();
    }

    @Override
    public boolean registerUser(RegistredUser registredNewAccount) {
        log.info("Registering new user: {} {}", registredNewAccount.getFirstName(),  registredNewAccount.getLastName());
        registredUserRepository.save(registredNewAccount);
        // Assuming email validation is handled elsewhere
        return true;
    }
}
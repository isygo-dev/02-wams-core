package eu.isygoit.service.impl;

import eu.isygoit.constants.AppParameterConstants;
import eu.isygoit.dto.request.AccessRequestDto;
import eu.isygoit.dto.request.RequestTrackingDto;
import eu.isygoit.dto.response.AccessResponseDto;
import eu.isygoit.dto.response.AuthResponseDto;
import eu.isygoit.enums.IEnumAuth;
import eu.isygoit.model.ConnectionTracking;
import eu.isygoit.model.RegistredUser;
import eu.isygoit.remote.kms.KmsPasswordService;
import eu.isygoit.repository.RegistredNewAccountRepository;
import eu.isygoit.security.CustomAuthentification;
import eu.isygoit.security.CustomUserDetails;
import eu.isygoit.service.IAccountService;
import eu.isygoit.service.IAppParameterService;
import eu.isygoit.service.IAuthService;
import eu.isygoit.service.IDomainService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;

/**
 * The type Auth service.
 */
@Slf4j
@Service
@Transactional
public class AuthService implements IAuthService {

    @Autowired
    private KmsPasswordService kmsPasswordService;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private IAccountService accountService;
    @Autowired
    private IDomainService domainService;
    @Autowired
    private IAppParameterService parameterService;
    @Autowired
    private RegistredNewAccountRepository registredNewAccountRepository;

    @Override
    public AuthResponseDto authenticate(RequestTrackingDto requestTracking, String domain, String userName, String application, String password, IEnumAuth.Types authType) {
        if (!domainService.isEnabled(domain)) {
            throw new AccountAuthenticationException("domain disabled: " + domain);
        }
        //Check if application is allowed for the user (if param is set)
        log.info("Authenticate {} from domain {} to application {}", userName, domain, application);
        String checkAppAllowed = parameterService.getValueByDomainAndName(domain, AppParameterConstants.IS_APP_ALLOWED, true, AppParameterConstants.NO);
        log.info("Checking if application {} is allowed {}", application, checkAppAllowed);
        if (AppParameterConstants.YES.equals(checkAppAllowed) && !accountService.checkIfApplicationAllowed(domain,
                userName,
                application)) {
            log.info("Application Not Allowed ...........................................");
            throw new ApplicationNotAllowedException(application + " for " + userName);
        }

        try {
            Authentication authentication = authenticationManager
                    .authenticate(new CustomAuthentification(new StringBuilder(userName)
                            .append("@").append(domain)
                            .append("@").append(authType),
                            password,
                            new ArrayList<>()));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            try {
                ResponseEntity<AccessResponseDto> result = kmsPasswordService.getAccess(//RequestContextDto.builder().build(),
                        AccessRequestDto.builder()
                                .domain(domain)
                                .userName(userName)
                                .application(application)
                                .isAdmin(((CustomUserDetails) authentication.getPrincipal()).getIsAdmin())
                                .password(password)
                                .authType(authType)
                                .authorities(((CustomUserDetails) authentication.getPrincipal()).getAuthorities().stream()
                                        .map(GrantedAuthority::getAuthority).toList())
                                .build());

                if (result.getStatusCode().is2xxSuccessful() && result.hasBody()) {
                    AccessResponseDto accessResponse = result.getBody();
                    switch (accessResponse.getStatus()) {
                        case VALID -> {
                            requestTracking.setAppOrigin(application);
                            this.trackUserConnections(domain,
                                    userName,
                                    application,
                                    requestTracking);
                            return AuthResponseDto.builder()
                                    .tokenType(accessResponse.getTokenType())
                                    .accessToken(accessResponse.getAccessToken())
                                    .refreshToken(accessResponse.getRefreshToken())
                                    .authorityToken(accessResponse.getAuthorityToken())
                                    .build();
                        }
                        case LOCKED -> {
                            throw new LockedPasswordException("with domain/username: " + domain + "/" + userName);
                        }
                        case EXPIRED -> {
                            throw new ExpiredPasswordException("with domain/username: " + domain + "/" + userName);
                        }
                        case DEPRECATED, BROKEN, BAD -> {
                            throw new DeprecatedPasswordException("with domain/username: " + domain + "/" + userName);
                        }
                        default -> throw new UnauthorizedException("with domain/username: " + domain + "/" + userName);
                    }
                } else {
                    throw new UnauthorizedException("with domain/username: " + domain + "/" + userName);
                }
            } catch (Exception e) {
                log.error("Remote feign call failed : ", e);
                throw new RemoteCallFailedException(e);
            }

        } catch (AuthenticationException e) {
            log.error("<Error>: Authentication failed for user: " + userName + "@" + domain, e);
            throw new AccountAuthenticationException("Authentication failed for user: " + userName + "@" + domain);
        }
    }

    private void trackUserConnections(String domain, String userName, String application, RequestTrackingDto requestTracking) {
        accountService.trackUserConnections(domain, userName, ConnectionTracking.builder()
                .device(requestTracking.getDevice())
                .browser(requestTracking.getBrowser())
                .ipAddress(requestTracking.getIpOrigin())
                .logApp(application)
                .loginDate(new Date())
                .build());
    }

    @Override
    public boolean registerNewAccount(RegistredUser registredNewAccount) {
        registredNewAccountRepository.save(registredNewAccount);
        //send validation email
        return true;
    }
}

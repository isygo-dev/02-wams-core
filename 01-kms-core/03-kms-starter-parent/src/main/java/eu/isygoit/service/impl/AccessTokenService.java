package eu.isygoit.service.impl;

import eu.isygoit.annotation.SrvRepo;
import eu.isygoit.app.ApplicationContextService;
import eu.isygoit.com.rest.service.impl.CrudService;
import eu.isygoit.config.AppProperties;
import eu.isygoit.enums.IEnumAppToken;
import eu.isygoit.model.AccessToken;
import eu.isygoit.repository.AccessTokenRepository;
import eu.isygoit.service.IAccessTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * The type Access token service.
 */
@Slf4j
@Service
@Transactional
@SrvRepo(value = AccessTokenRepository.class)
public class AccessTokenService extends CrudService<Long, AccessToken, AccessTokenRepository> implements IAccessTokenService {

    private final ApplicationContextService applicationContextService;
    @Override
    protected ApplicationContextService getApplicationContextServiceInstance() {
        return applicationContextService;
    }

    private final AppProperties appProperties;

    /**
     * Instantiates a new Access token service.
     *
     * @param appProperties the app properties
     */
    @Autowired
    public AccessTokenService(ApplicationContextService applicationContextService, AppProperties appProperties) {
        this.applicationContextService = applicationContextService;
        this.appProperties = appProperties;
    }

    public Optional<AccessToken> findByApplicationAndAccountCodeAndTokenAndTokenType(String application, String accountCode, String token, IEnumAppToken.Types tokenType) {
        return repository().findFirstByApplicationAndAccountCodeIgnoreCaseAndTokenAndTokenTypeAndDeprecatedFalseOrderByCreateDateDesc(application,
                accountCode, token, tokenType);
    }

    @Override
    public Optional<AccessToken> findByAccountCodeAndTokenAndTokenType(String accountCode, String token, IEnumAppToken.Types tokenType) {
        return repository().findFirstByAccountCodeIgnoreCaseAndTokenAndTokenTypeAndDeprecatedFalseOrderByCreateDateDesc(accountCode, token, tokenType);
    }

    @Override
    public AccessToken beforeCreate(AccessToken accessToken) {
        //Deactivate old tokens (user, token type, application)
        if (appProperties.isDeactivateOldTokens()) {
            repository().deactivateOldTokens(accessToken.getAccountCode(),
                    accessToken.getTokenType(),
                    accessToken.getApplication());
        }
        return accessToken;
    }
}

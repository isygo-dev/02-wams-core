package eu.isygoit.service.impl;

import eu.isygoit.annotation.InjectRepository;
import eu.isygoit.com.rest.service.CrudService;
import eu.isygoit.config.AppProperties;
import eu.isygoit.enums.IEnumToken;
import eu.isygoit.model.AccessToken;
import eu.isygoit.repository.AccessTokenRepository;
import eu.isygoit.service.IAccessTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * The type Access token service.
 */
@Slf4j
@Service
@Transactional
@InjectRepository(value = AccessTokenRepository.class)
public class AccessTokenService extends CrudService<Long, AccessToken, AccessTokenRepository> implements IAccessTokenService {

    private final AppProperties appProperties;

    /**
     * Instantiates a new Access token service.
     *
     * @param appProperties the app properties
     */
    public AccessTokenService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    //@Cacheable(cacheNames = SchemaTableConstantName.T_ACCESS_TOKEN, key = "{#application, #accountCode, #token, #tokenType}")
    public AccessToken findAccessToken(String application, String accountCode,
                                       Long crc16, Long crc32,
                                       IEnumToken.Types tokenType) {
        Optional<AccessToken> optional =
                repository().findFirstByApplicationAndAccountCodeIgnoreCaseAndCrc16AndCrc32AndTokenTypeAndDeprecatedFalseOrderByCreateDateDesc(
                        application, accountCode, crc16, crc32, tokenType);
        if (optional.isPresent()) {
            return optional.get();
        }
        return null;
    }

    @Override
    public AccessToken findAccessToken(String accountCode,
                                       Long crc16, Long crc32,
                                       IEnumToken.Types tokenType) {
        Optional<AccessToken> optional =
                repository().findFirstByAccountCodeIgnoreCaseAndCrc16AndCrc32AndTokenTypeAndDeprecatedFalseOrderByCreateDateDesc(
                        accountCode, crc16, crc32, tokenType);
        if (optional.isPresent()) {
            return optional.get();
        }
        return null;
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

    //@CachePut(cacheNames = SchemaTableConstantName.T_ACCESS_TOKEN, key = "{#accessToken.application, #accessToken.accountCode, #accessToken.token, #accessToken.tokenType}")
    @Override
    public AccessToken create(AccessToken accessToken) {
        return super.create(accessToken);
    }

    //@CachePut(cacheNames = SchemaTableConstantName.T_ACCESS_TOKEN, key = "{#accessToken.application, #accessToken.accountCode, #accessToken.token, #accessToken.tokenType}")
    @Override
    public AccessToken update(AccessToken accessToken) {
        return super.update(accessToken);
    }
}

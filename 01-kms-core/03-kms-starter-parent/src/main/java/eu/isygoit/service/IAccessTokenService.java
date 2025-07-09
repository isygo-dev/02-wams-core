package eu.isygoit.service;

import eu.isygoit.com.rest.service.ICrudServiceMethods;
import eu.isygoit.com.rest.service.tenancy.ICrudTenantServiceMethods;
import eu.isygoit.enums.IEnumToken;
import eu.isygoit.model.AccessToken;

/**
 * The interface Access token service.
 */
public interface IAccessTokenService extends ICrudServiceMethods<Long, AccessToken> {

    /**
     * Find by application and account code and token and token type access token.
     *
     * @param application the application
     * @param accountCode the account code
     * @param token       the token
     * @param tokenType   the token type
     * @return the access token
     */
    AccessToken findByApplicationAndAccountCodeAndTokenAndTokenType(String application, String accountCode, String token, IEnumToken.Types tokenType);

    /**
     * Find by account code and token and token type access token.
     *
     * @param accountCode the account code
     * @param token       the token
     * @param tokenType   the token type
     * @return the access token
     */
    AccessToken findByAccountCodeAndTokenAndTokenType(String accountCode, String token, IEnumToken.Types tokenType);
}

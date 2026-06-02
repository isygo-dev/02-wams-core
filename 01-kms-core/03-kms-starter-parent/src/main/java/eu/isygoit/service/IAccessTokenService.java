package eu.isygoit.service;

import eu.isygoit.com.rest.service.ICrudServiceOperations;
import eu.isygoit.enums.IEnumToken;
import eu.isygoit.model.AccessToken;

/**
 * The interface Access token service.
 */
public interface IAccessTokenService extends ICrudServiceOperations<Long, AccessToken> {

    /**
     * Find by application and account code and token and token type access token.
     *
     * @param application the application
     * @param accountCode the account code
     * @param tokenType   the token type
     * @return the access token
     */
    AccessToken findAccessToken(String application, String accountCode, Long crc16, Long crc32, IEnumToken.Types tokenType);

    /**
     * Find by account code and token and token type access token.
     *
     * @param accountCode the account code
     * @param tokenType   the token type
     * @return the access token
     */
    AccessToken findAccessToken(String accountCode, Long crc16, Long crc32, IEnumToken.Types tokenType);
}

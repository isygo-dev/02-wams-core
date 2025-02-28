package eu.isygoit.service;

import eu.isygoit.com.rest.service.ICrudServiceMethod;
import eu.isygoit.enums.IEnumToken;
import eu.isygoit.model.TokenConfig;

/**
 * The interface Token config service.
 */
public interface ITokenConfigService extends ICrudServiceMethod<Long, TokenConfig> {

    /**
     * Build token config token config.
     *
     * @param domain    the domain
     * @param tokenType the token type
     * @return the token config
     */
    TokenConfig buildTokenConfig(String domain, IEnumToken.Types tokenType);
}

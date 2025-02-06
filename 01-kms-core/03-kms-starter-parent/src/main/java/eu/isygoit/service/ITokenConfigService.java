package eu.isygoit.service;

import eu.isygoit.com.rest.service.ICrudServiceMethod;
import eu.isygoit.enums.IEnumAppToken;
import eu.isygoit.model.TokenConfig;

import java.util.Optional;

/**
 * The interface Token config service.
 */
public interface ITokenConfigService extends ICrudServiceMethod<Long, TokenConfig> {

    /**
     * Build token config optional.
     *
     * @param domain    the domain
     * @param tokenType the token type
     * @return the optional
     */
    Optional<TokenConfig> buildTokenConfig(String domain, IEnumAppToken.Types tokenType);
}

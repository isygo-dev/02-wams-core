package eu.isygoit.service;

import eu.isygoit.com.rest.service.ICrudServiceMethods;
import eu.isygoit.com.rest.service.tenancy.ICrudTenantServiceMethods;
import eu.isygoit.enums.IEnumToken;
import eu.isygoit.model.TokenConfig;

/**
 * The interface Token config service.
 */
public interface ITokenConfigService extends ICrudTenantServiceMethods<Long, TokenConfig> {

    /**
     * Build token config token config.
     *
     * @param tenant    the tenant
     * @param tokenType the token type
     * @return the token config
     */
    TokenConfig buildTokenConfig(String tenant, IEnumToken.Types tokenType);
}

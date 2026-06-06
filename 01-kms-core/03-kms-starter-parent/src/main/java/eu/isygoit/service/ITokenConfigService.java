package eu.isygoit.service;

import eu.isygoit.com.rest.service.tenancy.ICrudTenantServiceOperations;
import eu.isygoit.enums.IEnumToken;
import eu.isygoit.model.TokenConfig;

/**
 * The interface Token config service.
 */
public interface ITokenConfigService extends ICrudTenantServiceOperations<Long, TokenConfig> {

    /**
     * Build token config token config.
     *
     * @param tenant    the tenant
     * @param tokenType the token type
     * @param s
     * @return the token config
     */
    TokenConfig prepareTokenConfig(String tenant, IEnumToken.Types tokenType, String kmsKeyVersionId);

    TokenConfig fillSecretsWithCurrentKmsKeyVersion(String tenant, TokenConfig tokenConfig, String kmsKeyVersionId);
}

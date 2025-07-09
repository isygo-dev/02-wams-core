package eu.isygoit.repository;

import eu.isygoit.enums.IEnumToken;
import eu.isygoit.model.TokenConfig;
import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAndCodeAssignableRepository;

import java.util.Optional;

/**
 * The interface Token config repository.
 */
public interface TokenConfigRepository extends JpaPagingAndSortingTenantAndCodeAssignableRepository<TokenConfig, Long> {

    /**
     * Find by tenant ignore case and token type optional.
     *
     * @param tenant    the tenant
     * @param tokenType the token type
     * @return the optional
     */
    Optional<TokenConfig> findByTenantIgnoreCaseAndTokenType(String tenant, IEnumToken.Types tokenType);
}

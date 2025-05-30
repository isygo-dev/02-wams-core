package eu.isygoit.repository;

import eu.isygoit.enums.IEnumToken;
import eu.isygoit.model.TokenConfig;

import java.util.Optional;

/**
 * The interface Token config repository.
 */
public interface TokenConfigRepository extends JpaPagingAndSortingDomainAndCodeAssignableRepository<TokenConfig, Long> {

    /**
     * Find by domain ignore case and token type optional.
     *
     * @param domain    the domain
     * @param tokenType the token type
     * @return the optional
     */
    Optional<TokenConfig> findByDomainIgnoreCaseAndTokenType(String domain, IEnumToken.Types tokenType);
}

package eu.isygoit.repository;

import eu.isygoit.enums.IEnumAppToken;
import eu.isygoit.model.TokenConfig;

import java.util.Optional;

/**
 * The interface Token config repository.
 */
public interface TokenConfigRepository extends JpaPagingAndSortingSAASCodifiableRepository<TokenConfig, Long> {

    /**
     * Find by domain ignore case and token type optional.
     *
     * @param domain    the domain
     * @param tokenType the token type
     * @return the optional
     */
    Optional<TokenConfig> findByDomainIgnoreCaseAndTokenType(String domain, IEnumAppToken.Types tokenType);
}

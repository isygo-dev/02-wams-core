package eu.isygoit.repository;

import eu.isygoit.enums.IEnumAuth;
import eu.isygoit.model.PasswordConfig;

import java.util.Optional;

/**
 * The interface Password config repository.
 */
public interface PasswordConfigRepository extends JpaPagingAndSortingDomainAndCodeAssignableRepository<PasswordConfig, Long> {

    /**
     * Find by domain ignore case and type optional.
     *
     * @param domain the domain
     * @param type   the type
     * @return the optional
     */
    Optional<PasswordConfig> findByDomainIgnoreCaseAndType(String domain, IEnumAuth.Types type);
}

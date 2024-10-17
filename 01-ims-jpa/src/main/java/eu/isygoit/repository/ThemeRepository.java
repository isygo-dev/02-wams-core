package eu.isygoit.repository;

import eu.isygoit.model.Theme;
import org.springframework.stereotype.Repository;

import java.util.Optional;


/**
 * The interface Theme repository.
 */
@Repository
public interface ThemeRepository extends JpaPagingAndSortingRepository<Theme, Long> {
    /**
     * Find by account code ignore case and domain code ignore case optional.
     *
     * @param accountCode the account code
     * @param domainCode  the domain code
     * @return the optional
     */
    Optional<Theme> findByAccountCodeIgnoreCaseAndDomainCodeIgnoreCase(String accountCode, String domainCode);
}

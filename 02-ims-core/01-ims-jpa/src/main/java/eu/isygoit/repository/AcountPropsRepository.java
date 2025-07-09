package eu.isygoit.repository;

import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAndCodeAssignableRepository;
import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAndCodeAssignableRepository;
import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAssignableRepository;

import eu.isygoit.model.AccountProps;
import org.springframework.stereotype.Repository;

import java.util.Optional;


/**
 * The interface Acount props repository.
 */
@Repository
public interface AcountPropsRepository extends JpaPagingAndSortingRepository<AccountProps, Long> {

    /**
     * Find by account code ignore case optional.
     *
     * @param accountCode the account code
     * @return the optional
     */
    Optional<AccountProps> findByAccount_CodeIgnoreCase(String accountCode);
}

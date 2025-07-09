package eu.isygoit.repository;

import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAndCodeAssignableRepository;
import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAndCodeAssignableRepository;
import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAssignableRepository;

import eu.isygoit.model.AppNextCode;
import org.springframework.stereotype.Repository;


/**
 * The interface App next code repository.
 */
@Repository
public interface AppNextCodeRepository extends NextCodeRepository<AppNextCode, Long> {

}

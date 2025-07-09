package eu.isygoit.repository;

import eu.isygoit.enums.IEnumEmailTemplate;
import eu.isygoit.model.MsgTemplate;
import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAndCodeAssignableRepository;

import java.util.Optional;


/**
 * The interface Msg template repository.
 */
public interface MsgTemplateRepository extends JpaPagingAndSortingTenantAndCodeAssignableRepository<MsgTemplate, Long> {

    /**
     * Find by tenant ignore case and name optional.
     *
     * @param tenant the tenant
     * @param name   the name
     * @return the optional
     */
    Optional<MsgTemplate> findByTenantIgnoreCaseAndName(String tenant, IEnumEmailTemplate.Types name);
}

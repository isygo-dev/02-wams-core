package eu.isygoit.repository;

import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAndCodeAssignableRepository;
import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAndCodeAssignableRepository;
import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAssignableRepository;

import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.model.KmsDomain;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * The interface Domain repository.
 */
public interface DomainRepository extends JpaPagingAndSortingRepository<KmsDomain, Long> {

    /**
     * Find by name ignore case optional.
     *
     * @param name the name
     * @return the optional
     */
    Optional<KmsDomain> findByNameIgnoreCase(String name);

    /**
     * Update admin status int.
     *
     * @param tenant    the tenant
     * @param newStatus the new status
     * @return the int
     */
    @Modifying
    @Query("UPDATE KmsDomain d SET d.adminStatus = :newStatus WHERE d.name = :tenant")
    int updateAdminStatus(@Param("tenant") String tenant,
                          @Param("newStatus") IEnumEnabledBinaryStatus.Types newStatus);

    /**
     * Gets admin status.
     *
     * @param tenant the tenant
     * @return the admin status
     */
    @Query("select d.adminStatus from KmsDomain d where d.name = :tenant")
    IEnumEnabledBinaryStatus.Types getAdminStatus(@Param("tenant") String tenant);
}

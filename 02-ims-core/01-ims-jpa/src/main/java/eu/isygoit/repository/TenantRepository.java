package eu.isygoit.repository;

import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.model.Tenant;
import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAndCodeAssignableRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * The interface Domain repository.
 */
public interface TenantRepository extends JpaPagingAndSortingTenantAndCodeAssignableRepository<Tenant, Long> {

    /**
     * Update admin status by id int.
     *
     * @param id        the id
     * @param newStatus the new status
     * @return the int
     */
    @Modifying
    @Query("UPDATE Tenant d SET d.adminStatus = :newStatus WHERE d.id = :id")
    int updateAdminStatusById(@Param("id") Long id,
                              @Param("newStatus") IEnumEnabledBinaryStatus.Types newStatus);

    /**
     * Find by name ignore case optional.
     *
     * @param name the name
     * @return the optional
     */
    Optional<Tenant> findByNameIgnoreCase(String name);

    /**
     * Gets all names.
     *
     * @return the all names
     */
    @Query("select d.name from Tenant d")
    List<String> getAllNames();

    /**
     * Gets admin status.
     *
     * @return the admin status
     */
    @Query("select d.adminStatus from Tenant d where d.name = :tenant")
    IEnumEnabledBinaryStatus.Types getAdminStatus(@Param("tenant") String tenant);
}

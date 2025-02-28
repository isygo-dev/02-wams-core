package eu.isygoit.repository;

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
     * @param domain    the domain
     * @param newStatus the new status
     * @return the int
     */
    @Modifying
    @Query("UPDATE KmsDomain d SET d.adminStatus = :newStatus WHERE d.name = :domain")
    int updateAdminStatus(@Param("domain") String domain,
                          @Param("newStatus") IEnumEnabledBinaryStatus.Types newStatus);

    /**
     * Gets admin status.
     *
     * @param domain the domain
     * @return the admin status
     */
    @Query("select d.adminStatus from KmsDomain d where d.name = :domain")
    IEnumEnabledBinaryStatus.Types getAdminStatus(@Param("domain") String domain);
}

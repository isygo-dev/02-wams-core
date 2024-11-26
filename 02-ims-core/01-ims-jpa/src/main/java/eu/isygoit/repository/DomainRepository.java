package eu.isygoit.repository;

import eu.isygoit.enums.IEnumBinaryStatus;
import eu.isygoit.model.Domain;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * The interface Domain repository.
 */
public interface DomainRepository extends JpaPagingAndSortingSAASCodifiableRepository<Domain, Long> {

    /**
     * Update admin status by id int.
     *
     * @param id        the id
     * @param newStatus the new status
     * @return the int
     */
    @Modifying
    @Query("UPDATE Domain d SET d.adminStatus = :newStatus WHERE d.id = :id")
    int updateAdminStatusById(@Param("id") Long id,
                              @Param("newStatus") IEnumBinaryStatus.Types newStatus);

    /**
     * Find by name ignore case optional.
     *
     * @param name the name
     * @return the optional
     */
    Optional<Domain> findByNameIgnoreCase(String name);

    /**
     * Gets all names.
     *
     * @return the all names
     */
    @Query("select d.name from Domain d")
    List<String> getAllNames();

    /**
     * Gets admin status.
     *
     * @return the admin status
     */
    @Query("select d.adminStatus from Domain d where d.name = :domain")
    IEnumBinaryStatus.Types getAdminStatus(@Param("domain") String domain);
}

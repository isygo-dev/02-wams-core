package eu.isygoit.repository;

import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAndCodeAssignableRepository;
import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAndCodeAssignableRepository;
import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAssignableRepository;

import eu.isygoit.model.VCalendar;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * The interface V calendar repository.
 */
public interface VCalendarRepository extends JpaPagingAndSortingTenantAndCodeAssignableRepository<VCalendar, Long> {

    /**
     * Find by tenant ignore case and name optional.
     *
     * @param tenant the tenant
     * @param name   the name
     * @return the optional
     */
    Optional<VCalendar> findByTenantIgnoreCaseAndName(String tenant, String name);

    @Modifying
    @Query("update VCalendar u set u.locked = :locked  where u.id = :id")
    void updateLockedStatus(@Param("locked") boolean locked,
                            @Param("id") Long id);


}

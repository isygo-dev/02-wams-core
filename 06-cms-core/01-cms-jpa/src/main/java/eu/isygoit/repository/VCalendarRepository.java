package eu.isygoit.repository;

import eu.isygoit.model.VCalendar;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * The interface V calendar repository.
 */
public interface VCalendarRepository extends JpaPagingAndSortingSAASRepository<VCalendar, Long> {

    /**
     * Find by domain ignore case and name optional.
     *
     * @param domain the domain
     * @param name   the name
     * @return the optional
     */
    Optional<VCalendar> findByDomainIgnoreCaseAndName(String domain, String name);

    @Modifying
    @Query("update VCalendar u set u.locked = :locked  where u.id = :id")
    void updateLockedStatus(@Param("locked") boolean locked,
                            @Param("id") Long id);


}

package eu.isygoit.repository;

import eu.isygoit.model.VCalendarEvent;

import java.util.List;
import java.util.Optional;

/**
 * The interface V event repository.
 */
public interface VEventRepository extends JpaPagingAndSortingSAASCodifiableRepository<VCalendarEvent, Long> {

    /**
     * Find by name optional.
     *
     * @param name the name
     * @return the optional
     */
    Optional<VCalendarEvent> findByName(String name);

    /**
     * Find by domain ignore case and calendar list.
     *
     * @param domain   the domain
     * @param calendar the calendar
     * @return the list
     */
    List<VCalendarEvent> findByDomainIgnoreCaseAndCalendar(String domain, String calendar);

    /**
     * Find by domain ignore case and calendar and code ignore case optional.
     *
     * @param domain   the domain
     * @param calendar the calendar
     * @param Code     the code
     * @return the optional
     */
    Optional<VCalendarEvent> findByDomainIgnoreCaseAndCalendarAndCodeIgnoreCase(String domain, String calendar, String Code);
}

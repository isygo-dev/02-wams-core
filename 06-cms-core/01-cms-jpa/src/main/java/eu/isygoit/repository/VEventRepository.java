package eu.isygoit.repository;

import eu.isygoit.model.VCalendarEvent;
import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAndCodeAssignableRepository;

import java.util.List;
import java.util.Optional;

/**
 * The interface V event repository.
 */
public interface VEventRepository extends JpaPagingAndSortingTenantAndCodeAssignableRepository<VCalendarEvent, Long> {

    /**
     * Find by name optional.
     *
     * @param name the name
     * @return the optional
     */
    Optional<VCalendarEvent> findByName(String name);

    /**
     * Find by tenant ignore case and calendar list.
     *
     * @param tenant   the tenant
     * @param calendar the calendar
     * @return the list
     */
    List<VCalendarEvent> findByTenantIgnoreCaseAndCalendar(String tenant, String calendar);

    /**
     * Find by tenant ignore case and calendar and code ignore case optional.
     *
     * @param tenant   the tenant
     * @param calendar the calendar
     * @param Code     the code
     * @return the optional
     */
    Optional<VCalendarEvent> findByTenantIgnoreCaseAndCalendarAndCodeIgnoreCase(String tenant, String calendar, String Code);
}

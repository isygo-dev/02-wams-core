package eu.isygoit.service;

import eu.isygoit.com.rest.service.ICrudServiceMethod;
import eu.isygoit.model.VCalendarEvent;

import java.util.List;

/**
 * The interface Iv event service.
 */
public interface IVEventService extends ICrudServiceMethod<Long, VCalendarEvent> {

    /**
     * Find by domain and calendar list.
     *
     * @param domain   the domain
     * @param calendar the calendar
     * @return the list
     */
    List<VCalendarEvent> findByDomainAndCalendar(String domain, String calendar);
}

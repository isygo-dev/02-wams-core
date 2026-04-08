package eu.isygoit.service;

import eu.isygoit.com.rest.service.tenancy.ICrudTenantServiceOperations;
import eu.isygoit.model.VCalendarEvent;

import java.util.List;

/**
 * The interface Iv event service.
 */
public interface IVEventService extends ICrudTenantServiceOperations<Long, VCalendarEvent> {

    /**
     * Find by tenant and calendar list.
     *
     * @param tenant   the tenant
     * @param calendar the calendar
     * @return the list
     */
    List<VCalendarEvent> findByTenantAndCalendar(String tenant, String calendar);
}

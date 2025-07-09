package eu.isygoit.service;

import eu.isygoit.com.rest.service.tenancy.ICrudTenantServiceMethods;
import eu.isygoit.model.VCalendar;
import org.springframework.core.io.Resource;

import java.io.IOException;

/**
 * The interface Iv calendar service.
 */
public interface IVCalendarService extends ICrudTenantServiceMethods<Long, VCalendar> {

    /**
     * Find by tenant and name v calendar.
     *
     * @param tenant the tenant
     * @param name   the name
     * @return the v calendar
     */
    VCalendar findByTenantAndName(String tenant, String name);

    /**
     * Download resource.
     *
     * @param tenant the tenant
     * @param name   the name
     * @return the resource
     * @throws IOException the io exception
     */
    Resource download(String tenant, String name) throws IOException;

    VCalendar updateLockedStatus(Long id, Boolean locked) throws IOException;
}

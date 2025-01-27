package eu.isygoit.service;

import eu.isygoit.com.rest.service.ICrudServiceMethod;
import eu.isygoit.model.VCalendar;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.Optional;

/**
 * The interface Iv calendar service.
 */
public interface IVCalendarService extends ICrudServiceMethod<Long, VCalendar> {

    /**
     * Find by domain and name v calendar.
     *
     * @param domain the domain
     * @param name   the name
     * @return the v calendar
     */
    Optional<VCalendar> findByDomainAndName(String domain, String name);

    /**
     * Download resource.
     *
     * @param domain the domain
     * @param name   the name
     * @return the resource
     * @throws IOException the io exception
     */
    Resource download(String domain, String name) throws IOException;

    VCalendar updateLockedStatus(Long id, Boolean locked) throws IOException;
}

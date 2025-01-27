package eu.isygoit.service;

import eu.isygoit.com.rest.service.ICrudServiceMethod;
import eu.isygoit.com.rest.service.IImageServiceMethods;
import eu.isygoit.enums.IEnumBinaryStatus;
import eu.isygoit.model.Application;

import java.util.Optional;

/**
 * The interface Application service.
 */
public interface IApplicationService extends ICrudServiceMethod<Long, Application>, IImageServiceMethods<Long, Application> {

    /**
     * Update status application.
     *
     * @param id        the id
     * @param newStatus the new status
     * @return the application
     */
    Application updateStatus(Long id, IEnumBinaryStatus.Types newStatus);

    /**
     * Find by name application.
     *
     * @param name the name
     * @return the application
     */
    Optional<Application> findByName(String name);
}

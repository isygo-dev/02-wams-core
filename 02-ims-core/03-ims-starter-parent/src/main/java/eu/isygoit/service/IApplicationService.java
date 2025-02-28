package eu.isygoit.service;

import eu.isygoit.com.rest.service.ICrudServiceMethod;
import eu.isygoit.com.rest.service.IImageServiceMethods;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.model.Application;

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
    Application updateStatus(Long id, IEnumEnabledBinaryStatus.Types newStatus);

    /**
     * Find by name application.
     *
     * @param name the name
     * @return the application
     */
    Application findByName(String name);
}

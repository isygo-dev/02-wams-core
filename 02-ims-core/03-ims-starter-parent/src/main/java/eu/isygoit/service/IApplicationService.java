package eu.isygoit.service;

import eu.isygoit.com.rest.controller.impl.tenancy.IImageTenantServiceMethods;
import eu.isygoit.com.rest.service.tenancy.ICrudTenantServiceMethods;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.model.Application;

/**
 * The interface Application service.
 */
public interface IApplicationService extends ICrudTenantServiceMethods<Long, Application>, IImageTenantServiceMethods<Long, Application> {

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

package eu.isygoit.service;

import eu.isygoit.com.rest.service.ICrudServiceMethod;
import eu.isygoit.model.RoleInfo;

/**
 * The interface Role info service.
 */
public interface IRoleInfoService extends ICrudServiceMethod<Long, RoleInfo> {

    /**
     * Find by name role info.
     *
     * @param name the name
     * @return the role info
     */
    RoleInfo findByName(String name);

    /**
     * Find by code ignore case role info.
     *
     * @param code the code
     * @return the role info
     */
    RoleInfo findByCodeIgnoreCase(String code);
}

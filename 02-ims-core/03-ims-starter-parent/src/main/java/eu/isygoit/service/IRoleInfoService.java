package eu.isygoit.service;

import eu.isygoit.com.rest.service.ICrudServiceMethod;
import eu.isygoit.model.RoleInfo;

import java.util.Optional;

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
    Optional<RoleInfo> getByName(String name);

    /**
     * Find by code ignore case role info.
     *
     * @param code the code
     * @return the role info
     */
    Optional<RoleInfo> getByCode(String code);
}

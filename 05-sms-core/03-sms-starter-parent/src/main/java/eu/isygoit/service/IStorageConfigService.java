package eu.isygoit.service;

import eu.isygoit.com.rest.service.ICrudServiceMethods;
import eu.isygoit.com.rest.service.tenancy.ICrudTenantServiceMethods;
import eu.isygoit.model.StorageConfig;

/**
 * The interface Storage config service.
 */
public interface IStorageConfigService extends ICrudTenantServiceMethods<Long, StorageConfig> {

    /**
     * Find by tenant ignore case storage config.
     *
     * @param tenant the tenant
     * @return the storage config
     */
    StorageConfig findByTenantIgnoreCase(String tenant);
}

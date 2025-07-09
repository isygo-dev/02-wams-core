package eu.isygoit.service;

import eu.isygoit.com.rest.service.ICrudServiceMethods;
import eu.isygoit.com.rest.service.tenancy.ICrudTenantServiceMethods;
import eu.isygoit.model.SenderConfig;

/**
 * The interface Sender config service.
 */
public interface ISenderConfigService extends ICrudTenantServiceMethods<Long, SenderConfig> {
}

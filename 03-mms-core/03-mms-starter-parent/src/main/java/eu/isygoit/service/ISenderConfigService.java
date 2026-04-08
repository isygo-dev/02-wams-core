package eu.isygoit.service;

import eu.isygoit.com.rest.service.tenancy.ICrudTenantServiceOperations;
import eu.isygoit.model.SenderConfig;

/**
 * The interface Sender config service.
 */
public interface ISenderConfigService extends ICrudTenantServiceOperations<Long, SenderConfig> {
}

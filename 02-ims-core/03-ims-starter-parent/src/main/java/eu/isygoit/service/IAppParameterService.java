package eu.isygoit.service;

import eu.isygoit.com.rest.service.tenancy.ICrudTenantServiceMethods;
import eu.isygoit.model.AppParameter;


/**
 * The interface App parameter service.
 */
public interface IAppParameterService extends ICrudTenantServiceMethods<Long, AppParameter> {

    /**
     * Gets value by tenant and name.
     *
     * @param tenant       the tenant
     * @param name         the name
     * @param allowDefault the allow default
     * @param defaultValue the default value
     * @return the value by tenant and name
     */
    String getValueByTenantAndName(String tenant, String name, boolean allowDefault, String defaultValue);

    /**
     * Gets technical admin email.
     *
     * @return the technical admin email
     */
    String getTechnicalAdminEmail();
}

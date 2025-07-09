package eu.isygoit.service;

import eu.isygoit.com.rest.service.ICrudServiceMethods;
import eu.isygoit.com.rest.service.tenancy.ICrudTenantServiceMethods;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.model.Account;
import eu.isygoit.model.KmsDomain;

/**
 * The interface Domain service.
 */
public interface IDomainService extends ICrudServiceMethods<Long, KmsDomain> {

    /**
     * Check tenant if exists kms tenant.
     *
     * @param tenantName        the tenant name
     * @param tenantUrl         the tenant url
     * @param createIfNotExists the create if not exists
     * @return the kms tenant
     */
    KmsDomain checkDomainIfExists(String tenantName, String tenantUrl, boolean createIfNotExists);

    /**
     * Find by name ignore case kms tenant.
     *
     * @param tenantName the tenant name
     * @return the kms tenant
     */
    KmsDomain findByNameIgnoreCase(String tenantName);

    /**
     * Check account if exists account.
     *
     * @param tenantName        the tenant name
     * @param tenantUrl         the tenant url
     * @param email             the email
     * @param userName          the user name
     * @param fullName          the full name
     * @param createIfNotExists the create if not exists
     * @return the account
     */
    Account checkAccountIfExists(String tenantName, String tenantUrl, String email, String userName, String fullName, boolean createIfNotExists);

    /**
     * Check if exists boolean.
     *
     * @param kmsDomain         the kms tenant
     * @param createIfNotExists the create if not exists
     * @return the boolean
     */
    boolean checkIfExists(KmsDomain kmsDomain, boolean createIfNotExists);

    /**
     * Update admin status kms tenant.
     *
     * @param tenant    the tenant
     * @param newStatus the new status
     * @return the kms tenant
     */
    KmsDomain updateAdminStatus(String tenant, IEnumEnabledBinaryStatus.Types newStatus);

    /**
     * Is enabled boolean.
     *
     * @param tenant the tenant
     * @return the boolean
     */
    boolean isEnabled(String tenant);
}

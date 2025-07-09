package eu.isygoit.service;

import eu.isygoit.com.rest.controller.impl.tenancy.IImageTenantServiceMethods;
import eu.isygoit.com.rest.service.ICrudServiceMethods;
import eu.isygoit.com.rest.service.tenancy.ICrudTenantServiceMethods;
import eu.isygoit.com.rest.service.IImageServiceMethods;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.model.Domain;

import java.util.List;

/**
 * The interface Domain service.
 */
public interface IDomainService extends ICrudTenantServiceMethods<Long, Domain>, IImageTenantServiceMethods<Long, Domain> {

    /**
     * Gets all tenant names.
     *
     * @param tenant the tenant
     * @return the all tenant names
     */
    List<String> getAllDomainNames(String tenant);

    /**
     * Update admin status tenant.
     *
     * @param id        the id
     * @param newStatus the new status
     * @return the tenant
     */
    Domain updateAdminStatus(Long id, IEnumEnabledBinaryStatus.Types newStatus);

    /**
     * Gets image.
     *
     * @param tenantName the tenant name
     * @return the image
     */
    String getImage(String tenantName);

    /**
     * Find tenant idby tenant name long.
     *
     * @param name the name
     * @return the long
     */
    Long findDomainIdbyDomainName(String name);

    /**
     * Find by name tenant.
     *
     * @param name the name
     * @return the tenant
     */
    Domain findByName(String name);

    /**
     * Is enabled boolean.
     *
     * @param tenant the tenant
     * @return the boolean
     */
    boolean isEnabled(String tenant);

    /**
     * Update social link tenant.
     *
     * @param tenant the sender tenant
     * @param id           the id
     * @param social       the social
     * @param link         the link
     * @return the tenant
     */
    Domain updateSocialLink(String tenant, Long id, String social, String link);
}

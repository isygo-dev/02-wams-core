package eu.isygoit.service;

import eu.isygoit.com.rest.service.ICrudServiceMethod;
import eu.isygoit.com.rest.service.IImageServiceMethods;
import eu.isygoit.enums.IEnumBinaryStatus;
import eu.isygoit.model.Domain;

import java.util.List;

/**
 * The interface Domain service.
 */
public interface IDomainService extends ICrudServiceMethod<Long, Domain>, IImageServiceMethods<Long, Domain> {

    /**
     * Gets all domain names.
     *
     * @param domain the domain
     * @return the all domain names
     */
    List<String> getAllDomainNames(String domain);

    /**
     * Update admin status domain.
     *
     * @param id        the id
     * @param newStatus the new status
     * @return the domain
     */
    Domain updateAdminStatus(Long id, IEnumBinaryStatus.Types newStatus);

    /**
     * Gets image.
     *
     * @param domainName the domain name
     * @return the image
     */
    String getImage(String domainName);

    /**
     * Find domain idby domain name long.
     *
     * @param name the name
     * @return the long
     */
    Long findDomainIdbyDomainName(String name);

    /**
     * Find by name domain.
     *
     * @param name the name
     * @return the domain
     */
    Domain findByName(String name);

    /**
     * Is enabled boolean.
     *
     * @param domain the domain
     * @return the boolean
     */
    boolean isEnabled(String domain);

    /**
     * Update social link domain.
     *
     * @param senderDomain the sender domain
     * @param id           the id
     * @param social       the social
     * @param link         the link
     * @return the domain
     */
    Domain updateSocialLink(String senderDomain, Long id, String social, String link);
}

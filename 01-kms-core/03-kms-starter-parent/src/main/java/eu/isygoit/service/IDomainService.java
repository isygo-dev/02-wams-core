package eu.isygoit.service;

import eu.isygoit.com.rest.service.ICrudServiceMethod;
import eu.isygoit.enums.IEnumBinaryStatus;
import eu.isygoit.model.Account;
import eu.isygoit.model.KmsDomain;

import java.util.Optional;

/**
 * The interface Domain service.
 */
public interface IDomainService extends ICrudServiceMethod<Long, KmsDomain> {

    /**
     * Check if exists optional.
     *
     * @param domainName        the domain name
     * @param domainUrl         the domain url
     * @param createIfNotExists the create if not exists
     * @return the optional
     */
    Optional<KmsDomain> checkIfExists(String domainName, String domainUrl, boolean createIfNotExists);

    /**
     * Find by name ignore case optional.
     *
     * @param domainName the domain name
     * @return the optional
     */
    Optional<KmsDomain> findByNameIgnoreCase(String domainName);

    /**
     * Check account if exists optional.
     *
     * @param domainName        the domain name
     * @param domainUrl         the domain url
     * @param email             the email
     * @param userName          the user name
     * @param fullName          the full name
     * @param createIfNotExists the create if not exists
     * @return the optional
     */
    Optional<Account> checkAccountIfExists(String domainName, String domainUrl, String email, String userName, String fullName, boolean createIfNotExists);

    /**
     * Check if exists optional.
     *
     * @param kmsDomain         the kms domain
     * @param createIfNotExists the create if not exists
     * @return the optional
     */
    Optional<KmsDomain> checkIfExists(KmsDomain kmsDomain, boolean createIfNotExists);

    /**
     * Update admin status optional.
     *
     * @param domain    the domain
     * @param newStatus the new status
     * @return the optional
     */
    Optional<KmsDomain> updateAdminStatus(String domain, IEnumBinaryStatus.Types newStatus);

    /**
     * Is enabled boolean.
     *
     * @param domain the domain
     * @return the boolean
     */
    boolean isEnabled(String domain);
}

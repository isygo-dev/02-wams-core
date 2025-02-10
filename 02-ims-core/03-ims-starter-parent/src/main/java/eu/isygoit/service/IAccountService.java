package eu.isygoit.service;

import eu.isygoit.com.rest.service.ICrudServiceMethod;
import eu.isygoit.com.rest.service.IImageServiceMethods;
import eu.isygoit.dto.common.RequestContextDto;
import eu.isygoit.dto.data.*;
import eu.isygoit.dto.request.AccountAuthTypeRequest;
import eu.isygoit.dto.response.UserAccountDto;
import eu.isygoit.dto.response.UserContext;
import eu.isygoit.enums.IEnumBinaryStatus;
import eu.isygoit.enums.IEnumLanguage;
import eu.isygoit.enums.IEnumSharedStatType;
import eu.isygoit.exception.AccountNotFoundException;
import eu.isygoit.model.Account;
import eu.isygoit.model.Application;
import eu.isygoit.model.ConnectionTracking;
import jakarta.transaction.NotSupportedException;

import java.util.List;
import java.util.Optional;

/**
 * The interface Account service.
 */
public interface IAccountService extends ICrudServiceMethod<Long, Account>, IImageServiceMethods<Long, Account> {

    /**
     * Find by domain and user name account.
     *
     * @param domain   the domain
     * @param userName the user name
     * @return the account
     */
    Optional<Account> getByDomainAndUserName(String domain, String userName);

    /**
     * Find distinct allowed tools by domain and username list.
     *
     * @param domain   the domain
     * @param userName the username
     * @return the list
     */
    List<Application> getDistinctAllowedToolsByDomainAndUserName(String domain, String userName);

    /**
     * Update account admin status account.
     *
     * @param id        the id
     * @param newStatus the new status
     * @return the account
     */
    Account updateAdminStatus(Long id, IEnumBinaryStatus.Types newStatus);

    /**
     * Update account is admin account.
     *
     * @param id        the id
     * @param newStatus the new status
     * @return the account
     */
    Account updateIsAdmin(Long id, boolean newStatus);

    /**
     * Find emails by domain list.
     *
     * @param domain the domain
     * @return the list
     */
    List<String> findEmailsByDomain(String domain);

    /**
     * Build allowed tools list.
     *
     * @param account the account
     * @param token   the token
     * @return the list
     */
    List<ApplicationDto> buildAllowedTools(Account account, String token);


    /**
     * Gets min info by domain.
     *
     * @param domain the domain
     * @return the min info by domain
     * @throws NotSupportedException the not supported exception
     */
    List<MinAccountDto> getMinInfoByDomain(String domain) throws NotSupportedException;


    /**
     * Gets authentication type.
     *
     * @param accountAuthTypeRequest the account auth type request
     * @return the authentication type
     * @throws AccountNotFoundException the account not found exception
     */
    UserContext getAuthenticationType(AccountAuthTypeRequest accountAuthTypeRequest) throws AccountNotFoundException;

    /**
     * Switch auth type boolean.
     *
     * @param accountAuthTypeRequest the account auth type request
     * @return the boolean
     * @throws AccountNotFoundException the account not found exception
     */
    boolean switchAuthType(AccountAuthTypeRequest accountAuthTypeRequest) throws AccountNotFoundException;

    /**
     * Update language account.
     *
     * @param id       the id
     * @param language the language
     * @return the account
     */
    Account updateLanguage(Long id, IEnumLanguage.Types language);

    /**
     * Gets by domain.
     *
     * @param domain the domain
     * @return the by domain
     */
    List<Account> getByDomain(String domain);

    /**
     * Check if application allowed boolean.
     *
     * @param domain      the domain
     * @param userName    the user name
     * @param application the application
     * @return the boolean
     */
    boolean isApplicationAllowed(String domain, String userName, String application);

    /**
     * Track user connections.
     *
     * @param domain             the domain
     * @param userName           the user name
     * @param connectionTracking the connection tracking
     */
    void trackUserConnections(String domain, String userName, ConnectionTracking connectionTracking);

    /**
     * Chat accounts by domain list.
     *
     * @param domain the domain
     * @return the list
     */
    List<Account> getChatAccountsByDomain(String domain);

    /**
     * Resend creation email boolean.
     *
     * @param id the id
     * @return the boolean
     */
    boolean resendCreationEmail(Long id);

    /**
     * Gets global statistics.
     *
     * @param statType       the stat type
     * @param requestContext the request context
     * @return the global statistics
     */
    AccountGlobalStatDto getGlobalStatistics(IEnumSharedStatType.Types statType, RequestContextDto requestContext);

    /**
     * Gets object statistics.
     *
     * @param code the code
     * @return the object statistics
     */
    AccountStatDto getObjectStatistics(String code);

    /**
     * Stat get confirmed resume accounts count long.
     *
     * @param requestContext the request context
     * @return the long
     */
    Long stat_GetConfirmedResumeAccountsCount(RequestContextDto requestContext);

    /**
     * Stat get confirmed employee accounts count long.
     *
     * @param requestContext the request context
     * @return the long
     */
    Long stat_GetConfirmedEmployeeAccountsCount(RequestContextDto requestContext);

    /**
     * Create domain admin account.
     *
     * @param domain the domain
     * @param admin  the admin
     * @return the account
     */
    Account createDomainAdminAccount(String domain, DomainAdminDto admin);

    /**
     * Gets authentication data.
     *
     * @param email the email
     * @return the authentication data
     * @throws AccountNotFoundException the account not found exception
     */
    List<UserAccountDto> getAvailableEmailAccounts(String email) throws AccountNotFoundException;
}

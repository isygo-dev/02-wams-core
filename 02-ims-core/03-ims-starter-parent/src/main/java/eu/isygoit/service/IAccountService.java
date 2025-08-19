package eu.isygoit.service;

import eu.isygoit.com.rest.controller.impl.tenancy.IImageTenantServiceMethods;
import eu.isygoit.com.rest.service.tenancy.ICrudTenantServiceMethods;
import eu.isygoit.dto.common.ContextRequestDto;
import eu.isygoit.dto.data.*;
import eu.isygoit.dto.request.AccountAuthTypeRequest;
import eu.isygoit.dto.response.UserAccountDto;
import eu.isygoit.dto.response.UserContext;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.enums.IEnumLanguage;
import eu.isygoit.enums.IEnumSharedStatType;
import eu.isygoit.exception.AccountNotFoundException;
import eu.isygoit.model.Account;
import eu.isygoit.model.Application;
import eu.isygoit.model.ConnectionTracking;
import jakarta.transaction.NotSupportedException;

import java.util.List;

/**
 * The interface Account service.
 */
public interface IAccountService extends ICrudTenantServiceMethods<Long, Account>, IImageTenantServiceMethods<Long, Account> {

    /**
     * Find by tenant and user name account.
     *
     * @param tenant   the tenant
     * @param userName the user name
     * @return the account
     */
    Account findByTenantAndUserName(String tenant, String userName);

    /**
     * Find distinct allowed tools by tenant and username list.
     *
     * @param tenant   the tenant
     * @param userName the username
     * @return the list
     */
    List<Application> findDistinctAllowedToolsByTenantAndUserName(String tenant, String userName);

    /**
     * Update account admin status account.
     *
     * @param id        the id
     * @param newStatus the new status
     * @return the account
     */
    Account updateAccountAdminStatus(Long id, IEnumEnabledBinaryStatus.Types newStatus);

    /**
     * Update account is admin account.
     *
     * @param id        the id
     * @param newStatus the new status
     * @return the account
     */
    Account updateAccountIsAdmin(Long id, boolean newStatus);

    /**
     * Find emails by tenant list.
     *
     * @param tenant the tenant
     * @return the list
     */
    List<String> findEmailsByTenant(String tenant);

    /**
     * Build allowed tools list.
     *
     * @param account the account
     * @param token   the token
     * @return the list
     */
    List<ApplicationDto> buildAllowedTools(Account account, String token);


    /**
     * Gets min info by tenant.
     *
     * @param tenant the tenant
     * @return the min info by tenant
     * @throws NotSupportedException the not supported exception
     */
    List<MinAccountDto> getMinInfoByTenant(String tenant) throws NotSupportedException;


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
    boolean switchAuthType(String tenant, AccountAuthTypeRequest accountAuthTypeRequest) throws AccountNotFoundException;

    /**
     * Update language account.
     *
     * @param id       the id
     * @param language the language
     * @return the account
     */
    Account updateLanguage(Long id, IEnumLanguage.Types language);

    /**
     * Gets by tenant.
     *
     * @param tenant the tenant
     * @return the by tenant
     */
    List<Account> getByTenant(String tenant);

    /**
     * Check if application allowed boolean.
     *
     * @param tenant      the tenant
     * @param userName    the user name
     * @param application the application
     * @return the boolean
     */
    boolean checkIfApplicationAllowed(String tenant, String userName, String application);

    /**
     * Track user connections.
     *
     * @param tenant             the tenant
     * @param userName           the user name
     * @param connectionTracking the connection tracking
     */
    void trackUserConnections(String tenant, String userName, ConnectionTracking connectionTracking);

    /**
     * Chat accounts by tenant list.
     *
     * @param tenant the tenant
     * @return the list
     */
    List<Account> chatAccountsByTenant(String tenant);

    /**
     * Resend creation email boolean.
     *
     * @param id the id
     * @return the boolean
     */
    boolean resendCreationEmail(String tenant, Long id);

    /**
     * Gets global statistics.
     *
     * @param statType       the stat type
     * @param requestContext the request context
     * @return the global statistics
     */
    AccountGlobalStatDto getGlobalStatistics(IEnumSharedStatType.Types statType, ContextRequestDto requestContext);

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
    Long stat_GetConfirmedResumeAccountsCount(ContextRequestDto requestContext);

    /**
     * Stat get confirmed employee accounts count long.
     *
     * @param requestContext the request context
     * @return the long
     */
    Long stat_GetConfirmedEmployeeAccountsCount(ContextRequestDto requestContext);

    /**
     * Create tenant admin account.
     *
     * @param tenant the tenant
     * @param admin  the admin
     * @return the account
     */
    Account createDomainAdmin(String tenant, DomainAdminDto admin);

    /**
     * Gets authentication data.
     *
     * @param email the email
     * @return the authentication data
     * @throws AccountNotFoundException the account not found exception
     */
    List<UserAccountDto> getAvailableEmailAccounts(String email) throws AccountNotFoundException;
}

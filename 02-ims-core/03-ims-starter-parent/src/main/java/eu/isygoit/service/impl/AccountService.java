package eu.isygoit.service.impl;

import eu.isygoit.annotation.InjectCodeGen;
import eu.isygoit.annotation.InjectCodeGenKms;
import eu.isygoit.annotation.InjectRepository;
import eu.isygoit.com.rest.service.tenancy.ImageTenantService;
import eu.isygoit.config.AppProperties;
import eu.isygoit.constants.AccountTypeConstants;
import eu.isygoit.constants.AppParameterConstants;
import eu.isygoit.constants.JwtConstants;
import eu.isygoit.constants.TenantConstants;
import eu.isygoit.dto.common.RequestContextDto;
import eu.isygoit.dto.common.TokenRequestDto;
import eu.isygoit.dto.common.TokenResponseDto;
import eu.isygoit.dto.data.*;
import eu.isygoit.dto.request.AuthenticationContextRequest;
import eu.isygoit.dto.request.GeneratePwdRequestDto;
import eu.isygoit.dto.response.UserAccountDto;
import eu.isygoit.dto.response.UserContext;
import eu.isygoit.dto.wsocket.WsConnectDto;
import eu.isygoit.enums.*;
import eu.isygoit.exception.*;
import eu.isygoit.mapper.ApplicationMapper;
import eu.isygoit.mapper.MinAccountMapper;
import eu.isygoit.model.*;
import eu.isygoit.model.schema.SchemaColumnConstantName;
import eu.isygoit.remote.kms.KmsIncrementalKeyService;
import eu.isygoit.remote.kms.KmsPasswordService;
import eu.isygoit.remote.kms.KmsPublicPasswordService;
import eu.isygoit.remote.kms.KmsTokenService;
import eu.isygoit.remote.mms.MmsChatMessageService;
import eu.isygoit.repository.AccountRepository;
import eu.isygoit.repository.RegisteredUserRepository;
import eu.isygoit.service.IAccountService;
import eu.isygoit.service.IAppParameterService;
import eu.isygoit.service.IRoleInfoService;
import eu.isygoit.service.ITenantService;
import jakarta.transaction.NotSupportedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * The type Account service.
 */
@Slf4j
@Service
@Transactional
@InjectCodeGen(value = NextCodeService.class)
@InjectCodeGenKms(value = KmsIncrementalKeyService.class)
@InjectRepository(value = AccountRepository.class)
public class AccountService extends ImageTenantService<Long, Account, AccountRepository>
        implements IAccountService {

    private final AppProperties appProperties;

    @Autowired
    private KmsPasswordService kmsPasswordService;
    @Autowired
    private KmsPublicPasswordService kmsPublicPasswordService;
    @Autowired
    private KmsTokenService kmsTokenService;
    @Autowired
    private ApplicationMapper applicationMapper;
    @Autowired
    private MinAccountMapper minAccountMapper;
    @Autowired
    private ITenantService tenantService;
    @Autowired
    private IAppParameterService parameterService;
    @Autowired
    private MmsChatMessageService mmsChatMessageService;
    @Autowired
    private IRoleInfoService roleInfoService;

    /**
     * Instantiates a new Account service.
     *
     * @param appProperties                 the app properties
     * @param registredNewAccountRepository the registred new account repository
     */
    public AccountService(AppProperties appProperties, RegisteredUserRepository registredNewAccountRepository) {
        this.appProperties = appProperties;
    }

    @Override
    public AppNextCode initCodeGenerator() {
        return AppNextCode.builder()
                .tenant(TenantConstants.DEFAULT_TENANT_NAME)
                .entity(Account.class.getSimpleName())
                .attribute(SchemaColumnConstantName.C_CODE)
                .prefix("ACT")
                .valueLength(6L)
                .codeValue(1L)
                .increment(1)
                .build();
    }

    @Override
    public Account findByTenantAndUserName(String tenant /*senderTenant*/, String userName) {
        Optional<Account> optional = repository().findByTenantIgnoreCaseAndCodeIgnoreCase(tenant, userName);
        if (optional.isPresent()) {
            return optional.get();
        }
        return null;
    }

    @Override
    public List<String> findEmailsByTenant(String tenant /*senderTenant*/) {
        if (TenantConstants.SUPER_TENANT_NAME.equals(tenant)) {
            return repository().findDistinctEmails();
        } else {
            return repository().findDistinctEmailsByTenant(tenant);
        }
    }

    @Override
    public List<Application> findDistinctAllowedToolsByTenantAndUserName(String tenant /*senderTenant*/, String userName) {
        Account account = findByTenantAndUserName(tenant, userName);
        if (account != null) {
            List<Application> applications = new ArrayList<>();
            for (RoleInfo role : account.getRoleInfo()) {
                applications.addAll(role.getAllowedTools());
            }
            return applications.stream().distinct().toList();
        }
        return Collections.EMPTY_LIST;
    }

    @Override
    public Account updateAccountAdminStatus(Long id, IEnumEnabledBinaryStatus.Types newStatus) {
        repository().updateAccountAdminStatus(newStatus, id);
        return repository().findById(id).orElse(null);
    }


    @Override
    public Account updateAccountIsAdmin(Long id, boolean newStatus) {
        repository().updateAccountIsAdmin(newStatus, id);
        return repository().findById(id).orElse(null);
    }

    @Override
    public Account updateLanguage(Long id, IEnumLanguage.Types language) {
        repository().updateLanguage(language, id);
        return repository().findById(id).orElse(null);
    }

    public List<ApplicationDto> buildAllowedTools(Account account, String token) {
        List<Application> applications = new ArrayList<>();
        for (RoleInfo role : account.getRoleInfo()) {
            applications.addAll(role.getAllowedTools());
        }

        //Add param to load disabled applications
        String hideDisabledApp = parameterService.getValueByTenantAndName(account.getTenant(), AppParameterConstants.HIDE_DISABLED_APP, true, AppParameterConstants.NO);
        if (AppParameterConstants.YES.equals(hideDisabledApp)) {
            return applications.stream()
                    .filter(application -> IEnumEnabledBinaryStatus.Types.ENABLED == application.getAdminStatus())
                    .distinct()
                    .map(application -> {
                        ApplicationDto app = applicationMapper.entityToDto(application);
                        app.setToken(kmsTokenService.buildToken(account.getTenant(),
                                Set.of(application.getName()),
                                IEnumToken.Types.ACCESS,
                                TokenRequestDto.builder()
                                        .subject(account.getCode())
                                        .claims(Map.of(JwtConstants.JWT_SENDER_TENANT, account.getTenant(),
                                                JwtConstants.JWT_SENDER_ACCOUNT_TYPE, account.getAccountType(),
                                                JwtConstants.JWT_SENDER_USER, account.getCode(),
                                                JwtConstants.JWT_LOG_APP, application.getName()))
                                        .build()).getBody());
                        return app;
                    }).toList();
        } else {
            return applications.stream()//.filter(application -> IEnumEnabledBinaryStatus.Types.ENABLED == application.getAdminStatus())
                    .distinct().
                    map(application -> {
                        ApplicationDto app = applicationMapper.entityToDto(application);
                        app.setToken(kmsTokenService.buildToken(account.getTenant(),
                                Set.of(application.getName()),
                                IEnumToken.Types.ACCESS,
                                TokenRequestDto.builder()
                                        .subject(account.getCode())
                                        .claims(Map.of(JwtConstants.JWT_SENDER_TENANT, account.getTenant(),
                                                JwtConstants.JWT_SENDER_ACCOUNT_TYPE, account.getAccountType(),
                                                JwtConstants.JWT_SENDER_USER, account.getCode(),
                                                JwtConstants.JWT_LOG_APP, application.getName()))
                                        .build()).getBody());
                        return app;
                    }).toList();
        }
    }

    @Override
    public List<Account> getByTenant(String tenant /*senderTenant*/) {
        if (TenantConstants.SUPER_TENANT_NAME.equals(tenant)) {
            return repository().findAll();
        } else {
            return repository().findByTenantIgnoreCaseIn(Arrays.asList(tenant));
        }
    }

    @Override
    public List<MinAccountDto> getMinInfoByTenant(String tenant /*senderTenant*/) throws NotSupportedException {
        if (TenantConstants.SUPER_TENANT_NAME.equals(tenant)) {
            return minAccountMapper.listEntityToDto(findAll(tenant));
        } else {
            return minAccountMapper.listEntityToDto(findAll(tenant));
        }
    }

    @Override
    public UserContext resolveAuthContext(AuthenticationContextRequest authenticationContextRequest) throws AccountNotFoundException {
        //Remove left & right spaces
        authenticationContextRequest.setTenant(authenticationContextRequest.getTenant().trim());
        authenticationContextRequest.setUserName(authenticationContextRequest.getUserName().trim());

        if (!tenantService.isEnabled(authenticationContextRequest.getTenant())) {
            throw new AccountAuthenticationException("tenant disabled: " + authenticationContextRequest.getTenant());
        }

        Account account = findByTenantAndUserName(authenticationContextRequest.getTenant(), authenticationContextRequest.getUserName());
        if (account != null) {
            if (IEnumAuth.Types.OTP == account.getAuthType()) {
                try {
                    ResponseEntity<Integer> result = kmsPublicPasswordService.generateOtp(
                            GeneratePwdRequestDto.builder()
                                    .tenant(account.getTenant())
                                    .email(account.getEmail())
                                    .userName(account.getCode())
                                    .fullName(account.getFullName())
                                    .build());
                    if (result.getStatusCode().is2xxSuccessful() && result.hasBody()) {
                        return UserContext.builder()
                                .authTypeMode(IEnumAuth.Types.OTP)
                                .otpLength(result.getBody())
                                .build();
                    } else {
                        throw new AccountAuthenticationException("OTP code was not generated");
                    }
                } catch (Exception e) {
                    log.error("Remote feign call failed : ", e);
                    throw new RemoteCallFailedException(e);
                }

            } else if (IEnumAuth.Types.QRC == account.getAuthType()) {
                try {
                    ResponseEntity<TokenResponseDto> result = kmsTokenService.buildToken(account.getTenant(),
                            Set.of(IEnumAuth.Types.QRC.meaning()),
                            IEnumToken.Types.QRC,
                            TokenRequestDto.builder()
                                    .subject(account.getCode())
                                    .claims(Map.of(JwtConstants.JWT_SENDER_TENANT, authenticationContextRequest.getTenant(),
                                            JwtConstants.JWT_LOG_APP, IEnumAuth.Types.QRC.meaning(),
                                            JwtConstants.JWT_SENDER_USER, account.getCode()))
                                    .build());
                    if (result.getStatusCode().is2xxSuccessful() && result.hasBody()) {
                        return UserContext.builder().authTypeMode(IEnumAuth.Types.QRC)
                                .qrCodeToken(result.getBody().getToken())   //NOSONAR
                                .build();
                    }
                } catch (Exception e) {
                    log.error("Remote feign call failed : ", e);
                    throw new RemoteCallFailedException(e);
                }

                return UserContext.builder().authTypeMode(IEnumAuth.Types.QRC)
                        .qrCodeToken(null)
                        .build();
            } else {
                return UserContext.builder().authTypeMode(IEnumAuth.Types.PWD)
                        .build();
            }

        } else {
            throw new AccountNotFoundException("with tenant: " + authenticationContextRequest.getTenant() + " and username with " + authenticationContextRequest.getUserName());
        }
    }

    @Override
    public List<UserAccountDto> getAvailableEmailAccounts(String email) throws AccountNotFoundException {
        List<Account> accounts = repository().findByEmailIgnoreCase(email);

        if (CollectionUtils.isEmpty(accounts)) {
            throw new AccountNotFoundException("No accounts found for email: " + email);
        }

        return accounts.stream()
                .filter(account -> account.getAdminStatus().equals(IEnumEnabledBinaryStatus.Types.ENABLED))
                .map(account -> {
                    if (!tenantService.isEnabled(account.getTenant())) {
                        throw new AccountAuthenticationException("tenant disabled: " + account.getTenant());
                    }
                    return UserAccountDto.builder()
                            .code(account.getCode())
                            .tenant(account.getTenant())
                            .tenantId(tenantService.findByName(account.getTenant()).map(Tenant::getId).orElse(null))
                            .fullName(account.getFullName())
                            .functionRole(account.getFunctionRole())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public boolean switchAuthType(String tenant /*senderTenant*/, AuthenticationContextRequest authenticationContextRequest) throws AccountNotFoundException {
        Account account = findByTenantAndUserName(authenticationContextRequest.getTenant(), authenticationContextRequest.getUserName());
        if (account != null) {
            if (authenticationContextRequest.getAuthType() == null) {
                if (account.getAuthType().equals(IEnumAuth.Types.OTP)) {
                    account.setAuthType(IEnumAuth.Types.PWD);
                } else {
                    account.setAuthType(IEnumAuth.Types.OTP);
                }
            } else {
                account.setAuthType(authenticationContextRequest.getAuthType());
            }
            this.update(tenant, account);
            return true;
        } else {
            throw new AccountNotFoundException("with tenant: " + authenticationContextRequest.getTenant() + " and username with " + authenticationContextRequest.getUserName());
        }
    }

    @Override
    protected String getUploadDirectory() {
        return this.appProperties.getUploadDirectory();
    }

    @Override
    public boolean checkIfApplicationAllowed(String tenant /*senderTenant*/, String userName, String application) {
        //allow webapp-gw/gateway for all users
        if ("webapp-gw".equals(application)) {
            return true;
        }

        Optional<Account> optional = repository().findByTenantIgnoreCaseAndCodeIgnoreCase(tenant, userName);
        if (optional.isPresent()) {
            for (RoleInfo roleInfo : optional.get().getRoleInfo()) {
                if (roleInfo.getAllowedTools().stream().parallel().anyMatch(app -> (app.getName().equals(application) && IEnumEnabledBinaryStatus.Types.ENABLED == app.getAdminStatus()))) {
                    return true;
                }
            }
        } else {
            throw new AccountNotFoundException(tenant + "/" + userName);
        }

        return false;
    }

    @Override
    public void trackUserConnections(String tenant /*senderTenant*/, String userName, ConnectionTracking connectionTracking) {
        Account account = this.findByTenantAndUserName(tenant, userName);
        if (account != null) {
            if (CollectionUtils.isEmpty(account.getConnectionTracking())) {
                account.setConnectionTracking(new ArrayList<>());
            }

            account.getConnectionTracking().add(connectionTracking);
            this.saveOrUpdate(tenant, account);
        }
    }

    @Override
    public List<Account> chatAccountsByTenant(String tenant /*senderTenant*/) {
        List<Account> list = repository().findByTenantIgnoreCaseIn(Arrays.asList(tenant, TenantConstants.SUPER_TENANT_NAME));
        if (CollectionUtils.isEmpty(list)) {
            return Collections.EMPTY_LIST;
        }
        try {
            Optional<Tenant> optionalTenant = tenantService.findByName(tenant);
            ResponseEntity<List<WsConnectDto>> result = mmsChatMessageService.getChatStatus(
                    optionalTenant.map(Tenant::getId).orElse(null));
            if (result.getStatusCode().is2xxSuccessful() && result.hasBody()) {
                List<WsConnectDto> connections = result.getBody();
                if (!CollectionUtils.isEmpty(connections)) {
                    list.stream().forEach(account -> {
                        Optional<WsConnectDto> optional = connections.stream()
                                .filter(wsConnectDto -> wsConnectDto.getSenderId().equals(account.getId()))
                                .findFirst();
                        if (optional.isPresent()) {
                            account.setChatStatus(optional.get().getStatus());
                        }
                    });
                }
            }
        } catch (Exception e) {
            log.error("Remote feign call failed : ", e);
            //throw new RemoteCallFailedException(e);
        }

        return list;
    }

    @Override
    public boolean resendCreationEmail(String tenant /*senderTenant*/, Long id) {
        try {
            Optional<Account> optional = this.findById(tenant, id);
            if (optional.isPresent()) {
                Account account = optional.get();
                Optional<Tenant> optionalTenant = tenantService.findByName(account.getTenant());
                ResponseEntity<Integer> result = kmsPasswordService.generatePwd(
                        GeneratePwdRequestDto.builder()
                                .tenant(account.getTenant())
                                .tenantUrl(optionalTenant.map(Tenant::getUrl).orElse(null))
                                .email(account.getEmail())
                                .userName(account.getCode())
                                .fullName(account.getFullName())
                                .build());
                if (result.getStatusCode().is2xxSuccessful() && result.hasBody()) {
                    return true;
                } else {
                    throw new SendEmailException("Account email reminder");
                }
            } else {
                throw new AccountNotFoundException("with id " + id);
            }
        } catch (Exception e) {
            log.error("Remote feign call failed : ", e);
            throw new RemoteCallFailedException(e);
        }
    }

    @Override
    public AccountGlobalStatDto getGlobalStatistics(IEnumSharedStatType.Types statType, RequestContextDto requestContext) {
        AccountGlobalStatDto.AccountGlobalStatDtoBuilder builder = AccountGlobalStatDto.builder();
        switch (statType) {
            case TOTAL_COUNT:
                builder.numberOfElements(stat_GetAccountsCount(requestContext));
                break;
            case ACTIVE_COUNT:
                builder.activeCount(stat_GetActiveAccountsCount(requestContext));
                break;
            case CONFIRMED_COUNT:
                builder.confirmedCount(stat_GetConfirmedAccountsCount(requestContext));
                break;
            case ADMINS_COUNT:
                builder.adminsCount(stat_GetAdminsCount(requestContext));
                break;
            default:
                throw new StatisticTypeNotSupportedException(statType.name());
        }

        return builder.build();
    }

    private Long stat_GetAdminsCount(RequestContextDto requestContext) {
        if (TenantConstants.SUPER_TENANT_NAME.equals(requestContext.getSenderTenant())) {
            return repository().countByIsAdminTrue();
        } else {
            return repository().countByTenantIgnoreCaseAndIsAdminTrue(requestContext.getSenderTenant());
        }
    }

    private Long stat_GetAccountsCount(RequestContextDto requestContext) {
        if (TenantConstants.SUPER_TENANT_NAME.equals(requestContext.getSenderTenant())) {
            return repository().count();
        } else {
            return repository().countByTenantIgnoreCase(requestContext.getSenderTenant());
        }
    }

    private Long stat_GetActiveAccountsCount(RequestContextDto requestContext) {
        if (TenantConstants.SUPER_TENANT_NAME.equals(requestContext.getSenderTenant())) {
            return repository().countByAdminStatus(IEnumEnabledBinaryStatus.Types.ENABLED);
        } else {
            return repository().countByTenantIgnoreCaseAndAdminStatus(requestContext.getSenderTenant(), IEnumEnabledBinaryStatus.Types.ENABLED);
        }
    }

    private Long stat_GetConfirmedAccountsCount(RequestContextDto requestContext) {
        if (TenantConstants.SUPER_TENANT_NAME.equals(requestContext.getSenderTenant())) {
            return repository().countByOrigin("SYS_ADMIN%");
        } else {
            return repository().countByTenantAndOrigin(requestContext.getSenderTenant(), "SYS_ADMIN%");
        }
    }

    @Override
    public Long stat_GetConfirmedResumeAccountsCount(RequestContextDto requestContext) {
        if (TenantConstants.SUPER_TENANT_NAME.equals(requestContext.getSenderTenant())) {
            return repository().countByOrigin("RESUME%");
        } else {
            return repository().countByTenantAndOrigin(requestContext.getSenderTenant(), "RESUME%");
        }
    }

    @Override
    public Long stat_GetConfirmedEmployeeAccountsCount(RequestContextDto requestContext) {
        if (TenantConstants.SUPER_TENANT_NAME.equals(requestContext.getSenderTenant())) {
            return repository().countByOrigin("EMPLOYEE%");
        } else {
            return repository().countByTenantAndOrigin(requestContext.getSenderTenant(), "EMPLOYEE%");
        }
    }

    @Override
    public Account createTenantAdmin(String tenant /*senderTenant*/, TenantAdminDto admin) {
        RoleInfo tenantAdmin = roleInfoService.findByName(AccountTypeConstants.TENANT_ADMIN);

        return this.create(tenant, Account.builder()
                .tenant(tenant)
                .isAdmin(Boolean.TRUE)
                .phoneNumber(admin.getPhone())
                .email(admin.getEmail())
                .accountDetails(AccountDetails.builder()
                        .firstName(admin.getFirstName())
                        .lastName(admin.getLastName())
                        .build())
                .functionRole("Tenant administrator")
                .roleInfo(Arrays.asList(tenantAdmin))
                .build());
    }

    @Override
    public AccountStatDto getObjectStatistics(String code) {
        return AccountStatDto.builder().build();
    }

    /**
     * Gets all accounts min.
     *
     * @return the all accounts min
     */
    //@Cacheable(cacheNames = SchemaTableConstantName.T_ACCOUNT)
    public List<MinAccountDto> getAllAccountsMin(String tenant /*senderTenant*/) {
        return minAccountMapper.listEntityToDto(this.findAll(tenant));
    }
}

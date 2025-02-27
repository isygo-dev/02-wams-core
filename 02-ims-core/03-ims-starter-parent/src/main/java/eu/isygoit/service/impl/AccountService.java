package eu.isygoit.service.impl;

import eu.isygoit.annotation.CodeGenKms;
import eu.isygoit.annotation.CodeGenLocal;
import eu.isygoit.annotation.SrvRepo;
import eu.isygoit.com.rest.service.ImageService;
import eu.isygoit.config.AppProperties;
import eu.isygoit.constants.AccountTypeConstants;
import eu.isygoit.constants.AppParameterConstants;
import eu.isygoit.constants.DomainConstants;
import eu.isygoit.constants.JwtConstants;
import eu.isygoit.dto.common.RequestContextDto;
import eu.isygoit.dto.common.TokenDto;
import eu.isygoit.dto.data.*;
import eu.isygoit.dto.request.AccountAuthTypeRequest;
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
import eu.isygoit.remote.kms.KmsTokenService;
import eu.isygoit.remote.mms.MmsChatMessageService;
import eu.isygoit.repository.AccountRepository;
import eu.isygoit.repository.RegistredUserRepository;
import eu.isygoit.service.IAccountService;
import eu.isygoit.service.IAppParameterService;
import eu.isygoit.service.IDomainService;
import eu.isygoit.service.IRoleInfoService;
import jakarta.transaction.NotSupportedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
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
@CodeGenLocal(value = NextCodeService.class)
@CodeGenKms(value = KmsIncrementalKeyService.class)
@SrvRepo(value = AccountRepository.class)
public class AccountService extends ImageService<Long, Account, AccountRepository>
        implements IAccountService {

    private final AppProperties appProperties;

    @Autowired
    private KmsPasswordService kmsPasswordService;
    @Autowired
    private KmsTokenService kmsTokenService;
    @Autowired
    private ApplicationMapper applicationMapper;
    @Autowired
    private MinAccountMapper minAccountMapper;
    @Autowired
    private IDomainService domainService;
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
    public AccountService(AppProperties appProperties, RegistredUserRepository registredNewAccountRepository) {
        this.appProperties = appProperties;
    }

    @Override
    public AppNextCode initCodeGenerator() {
        return AppNextCode.builder()
                .domain(DomainConstants.DEFAULT_DOMAIN_NAME)
                .entity(Account.class.getSimpleName())
                .attribute(SchemaColumnConstantName.C_CODE)
                .prefix("ACT")
                .valueLength(6L)
                .value(1L)
                .increment(1)
                .build();
    }

    @Override
    public Account findByDomainAndUserName(String domain, String userName) {
        Optional<Account> optional = repository().findByDomainIgnoreCaseAndCodeIgnoreCase(domain, userName);
        if (optional.isPresent()) {
            return optional.get();
        }
        return null;
    }

    @Override
    public List<String> findEmailsByDomain(String domain) {
        if (DomainConstants.SUPER_DOMAIN_NAME.equals(domain)) {
            return repository().findDistinctEmails();
        } else {
            return repository().findDistinctEmailsByDomain(domain);
        }
    }

    @Override
    public List<Application> findDistinctAllowedToolsByDomainAndUserName(String domain, String userName) {
        Account account = findByDomainAndUserName(domain, userName);
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
    public Account updateAccountAdminStatus(Long id, IEnumBinaryStatus.Types newStatus) {
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
        String hideDisabledApp = parameterService.getValueByDomainAndName(account.getDomain(), AppParameterConstants.HIDE_DISABLED_APP, true, AppParameterConstants.NO);
        if (AppParameterConstants.YES.equals(hideDisabledApp)) {
            return applications.stream()
                    .filter(application -> IEnumBinaryStatus.Types.ENABLED == application.getAdminStatus())
                    .distinct()
                    .map(application -> {
                        ApplicationDto app = applicationMapper.entityToDto(application);
                        app.setToken(kmsTokenService.createTokenByDomain(//RequestContextDto.builder().build(),
                                account.getDomain(),
                                application.getName(),
                                IEnumAppToken.Types.ACCESS,
                                TokenRequestDto.builder()
                                        .subject(account.getCode())
                                        .claims(Map.of(JwtConstants.JWT_SENDER_DOMAIN, account.getDomain(),
                                                JwtConstants.JWT_SENDER_ACCOUNT_TYPE, account.getAccountType(),
                                                JwtConstants.JWT_SENDER_USER, account.getCode(),
                                                JwtConstants.JWT_LOG_APP, application.getName()))
                                        .build()).getBody());
                        return app;
                    }).toList();
        } else {
            return applications.stream()//.filter(application -> IEnumBinaryStatus.Types.ENABLED == application.getAdminStatus())
                    .distinct().
                    map(application -> {
                        ApplicationDto app = applicationMapper.entityToDto(application);
                        app.setToken(kmsTokenService.createTokenByDomain(//RequestContextDto.builder().build(),
                                account.getDomain(),
                                application.getName(),
                                IEnumAppToken.Types.ACCESS,
                                TokenRequestDto.builder()
                                        .subject(account.getCode())
                                        .claims(Map.of(JwtConstants.JWT_SENDER_DOMAIN, account.getDomain(),
                                                JwtConstants.JWT_SENDER_ACCOUNT_TYPE, account.getAccountType(),
                                                JwtConstants.JWT_SENDER_USER, account.getCode(),
                                                JwtConstants.JWT_LOG_APP, application.getName()))
                                        .build()).getBody());
                        return app;
                    }).toList();
        }
    }

    @Override
    public List<Account> getByDomain(String domain) {
        if (DomainConstants.SUPER_DOMAIN_NAME.equals(domain)) {
            return repository().findAll();
        } else {
            return repository().findByDomainIgnoreCaseIn(Arrays.asList(domain));
        }
    }

    @Override
    public List<MinAccountDto> getMinInfoByDomain(String domain) throws NotSupportedException {
        if (DomainConstants.SUPER_DOMAIN_NAME.equals(domain)) {
            return minAccountMapper.listEntityToDto(findAll());
        } else {
            return minAccountMapper.listEntityToDto(findAll(domain));
        }
    }

    @Override
    public UserContext getAuthenticationType(AccountAuthTypeRequest accountAuthTypeRequest) throws AccountNotFoundException {
        //Remove left & right spaces
        accountAuthTypeRequest.setDomain(accountAuthTypeRequest.getDomain().trim());
        accountAuthTypeRequest.setUserName(accountAuthTypeRequest.getUserName().trim());

        if (!domainService.isEnabled(accountAuthTypeRequest.getDomain())) {
            throw new AccountAuthenticationException("domain disabled: " + accountAuthTypeRequest.getDomain());
        }

        Account account = findByDomainAndUserName(accountAuthTypeRequest.getDomain(), accountAuthTypeRequest.getUserName());
        if (account != null) {
            if (IEnumAuth.Types.OTP == account.getAuthType()) {
                try {
                    ResponseEntity<Integer> result = kmsPasswordService.generate(//RequestContextDto.builder().build(),
                            IEnumAuth.Types.OTP,
                            GeneratePwdRequestDto.builder()
                                    .domain(account.getDomain())
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
                    ResponseEntity<TokenDto> result = kmsTokenService.createTokenByDomain(//RequestContextDto.builder().build(),
                            account.getDomain(),
                            IEnumAuth.Types.QRC.meaning(),
                            IEnumAppToken.Types.QRC,
                            TokenRequestDto.builder()
                                    .subject(account.getCode())
                                    .claims(Map.of(JwtConstants.JWT_SENDER_DOMAIN, accountAuthTypeRequest.getDomain(),
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
            throw new AccountNotFoundException("with domain: " + accountAuthTypeRequest.getDomain() + " and username with " + accountAuthTypeRequest.getUserName());
        }
    }

    @Override
    public List<UserAccountDto> getAvailableEmailAccounts(String email) throws AccountNotFoundException {
        List<Account> accounts = repository().findByEmailIgnoreCase(email);

        if (CollectionUtils.isEmpty(accounts)) {
            throw new AccountNotFoundException("No accounts found for email: " + email);
        }

        return accounts.stream()
                .filter(account -> account.getAdminStatus().equals(IEnumBinaryStatus.Types.ENABLED))
                .map(account -> {
                    if (!domainService.isEnabled(account.getDomain())) {
                        throw new AccountAuthenticationException("domain disabled: " + account.getDomain());
                    }
                    return UserAccountDto.builder()
                            .code(account.getCode())
                            .domain(account.getDomain())
                            .domainId(domainService.findByName(account.getDomain()).getId())
                            .fullName(account.getFullName())
                            .functionRole(account.getFunctionRole())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public boolean switchAuthType(AccountAuthTypeRequest accountAuthTypeRequest) throws AccountNotFoundException {
        Account account = findByDomainAndUserName(accountAuthTypeRequest.getDomain(), accountAuthTypeRequest.getUserName());
        if (account != null) {
            if (accountAuthTypeRequest.getAuthType() == null) {
                if (account.getAuthType().equals(IEnumAuth.Types.OTP)) {
                    account.setAuthType(IEnumAuth.Types.PWD);
                } else {
                    account.setAuthType(IEnumAuth.Types.OTP);
                }
            } else {
                account.setAuthType(accountAuthTypeRequest.getAuthType());
            }
            this.update(account);
            return true;
        } else {
            throw new AccountNotFoundException("with domain: " + accountAuthTypeRequest.getDomain() + " and username with " + accountAuthTypeRequest.getUserName());
        }
    }

    @Override
    protected String getUploadDirectory() {
        return this.appProperties.getUploadDirectory();
    }

    @Override
    public boolean checkIfApplicationAllowed(String domain, String userName, String application) {
        //allow webapp-gw/gateway for all users
        if ("webapp-gw".equals(application)) {
            return true;
        }

        Optional<Account> optional = repository().findByDomainIgnoreCaseAndCodeIgnoreCase(domain, userName);
        if (optional.isPresent()) {
            for (RoleInfo roleInfo : optional.get().getRoleInfo()) {
                if (roleInfo.getAllowedTools().stream().parallel().anyMatch(app -> (app.getName().equals(application) && IEnumBinaryStatus.Types.ENABLED == app.getAdminStatus()))) {
                    return true;
                }
            }
        } else {
            throw new AccountNotFoundException(domain + "/" + userName);
        }

        return false;
    }

    @Override
    public void trackUserConnections(String domain, String userName, ConnectionTracking connectionTracking) {
        Account account = this.findByDomainAndUserName(domain, userName);
        if (account != null) {
            if (CollectionUtils.isEmpty(account.getConnectionTracking())) {
                account.setConnectionTracking(new ArrayList<>());
            }

            account.getConnectionTracking().add(connectionTracking);
            this.saveOrUpdate(account);
        }
    }

    @Override
    public List<Account> chatAccountsByDomain(String domain) {
        List<Account> list = repository().findByDomainIgnoreCaseIn(Arrays.asList(domain, DomainConstants.SUPER_DOMAIN_NAME));
        if (CollectionUtils.isEmpty(list)) {
            return Collections.EMPTY_LIST;
        }
        try {
            ResponseEntity<List<WsConnectDto>> result = mmsChatMessageService.getChatStatus(RequestContextDto.builder().build(),
                    domainService.findByName(domain).getId());
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
    public boolean resendCreationEmail(Long id) {
        try {
            Optional<Account> optional = this.findById(id);
            if(optional.isPresent()) {
                Account account = optional.get();
                ResponseEntity<Integer> result = kmsPasswordService.generate(//RequestContextDto.builder().build(),
                        IEnumAuth.Types.PWD,
                        GeneratePwdRequestDto.builder()
                                .domain(account.getDomain())
                                .domainUrl(domainService.findByName(account.getDomain()).getUrl())
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
                builder.totalCount(stat_GetAccountsCount(requestContext));
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
        if (DomainConstants.SUPER_DOMAIN_NAME.equals(requestContext.getSenderDomain())) {
            return repository().countByIsAdminTrue();
        } else {
            return repository().countByDomainIgnoreCaseAndIsAdminTrue(requestContext.getSenderDomain());
        }
    }

    private Long stat_GetAccountsCount(RequestContextDto requestContext) {
        if (DomainConstants.SUPER_DOMAIN_NAME.equals(requestContext.getSenderDomain())) {
            return repository().count();
        } else {
            return repository().countByDomainIgnoreCase(requestContext.getSenderDomain());
        }
    }

    private Long stat_GetActiveAccountsCount(RequestContextDto requestContext) {
        if (DomainConstants.SUPER_DOMAIN_NAME.equals(requestContext.getSenderDomain())) {
            return repository().countByAdminStatus(IEnumBinaryStatus.Types.ENABLED);
        } else {
            return repository().countByDomainIgnoreCaseAndAdminStatus(requestContext.getSenderDomain(), IEnumBinaryStatus.Types.ENABLED);
        }
    }

    private Long stat_GetConfirmedAccountsCount(RequestContextDto requestContext) {
        if (DomainConstants.SUPER_DOMAIN_NAME.equals(requestContext.getSenderDomain())) {
            return repository().countByOrigin("SYS_ADMIN%");
        } else {
            return repository().countByDomainAndOrigin(requestContext.getSenderDomain(), "SYS_ADMIN%");
        }
    }

    @Override
    public Long stat_GetConfirmedResumeAccountsCount(RequestContextDto requestContext) {
        if (DomainConstants.SUPER_DOMAIN_NAME.equals(requestContext.getSenderDomain())) {
            return repository().countByOrigin("RESUME%");
        } else {
            return repository().countByDomainAndOrigin(requestContext.getSenderDomain(), "RESUME%");
        }
    }

    @Override
    public Long stat_GetConfirmedEmployeeAccountsCount(RequestContextDto requestContext) {
        if (DomainConstants.SUPER_DOMAIN_NAME.equals(requestContext.getSenderDomain())) {
            return repository().countByOrigin("EMPLOYEE%");
        } else {
            return repository().countByDomainAndOrigin(requestContext.getSenderDomain(), "EMPLOYEE%");
        }
    }

    @Override
    public Account createDomainAdmin(String domain, DomainAdminDto admin) {
        RoleInfo domainAdmin = roleInfoService.findByName(AccountTypeConstants.DOMAIN_ADMIN);

        return this.create(Account.builder()
                .domain(domain)
                .isAdmin(Boolean.TRUE)
                .phoneNumber(admin.getPhone())
                .email(admin.getEmail())
                .accountDetails(AccountDetails.builder()
                        .firstName(admin.getFirstName())
                        .lastName(admin.getLastName())
                        .build())
                .functionRole("Domain administrator")
                .roleInfo(Arrays.asList(domainAdmin))
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
    @EventListener(ApplicationReadyEvent.class)
    public List<MinAccountDto> getAllAccountsMin() {
        return minAccountMapper.listEntityToDto(this.findAll());
    }
}

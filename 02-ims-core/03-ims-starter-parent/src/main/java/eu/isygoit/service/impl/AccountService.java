package eu.isygoit.service.impl;

import eu.isygoit.annotation.InjectCodeGen;
import eu.isygoit.annotation.InjectCodeGenKms;
import eu.isygoit.annotation.InjectRepository;
import eu.isygoit.com.rest.service.tenancy.ImageTenantService;
import eu.isygoit.config.AppProperties;
import eu.isygoit.constants.*;
import eu.isygoit.dto.common.RequestContextDto;
import eu.isygoit.dto.common.TokenRequestDto;
import eu.isygoit.dto.common.TokenResponseDto;
import eu.isygoit.dto.data.*;
import eu.isygoit.dto.request.AuthenticationContextRequest;
import eu.isygoit.dto.request.AuthenticationRequestDto;
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
import eu.isygoit.repository.QrLoginChallengeRepository;
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

import java.security.SecureRandom;
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

    private static final SecureRandom QR_CHALLENGE_RANDOM = new SecureRandom();
    private static final long QR_CHALLENGE_TTL_MILLIS = 120_000L;
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
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private QrLoginChallengeRepository qrLoginChallengeRepository;

    /**
     * Instantiates a new Account service.
     *
     * @param appProperties                 the app properties
     * @param registredNewAccountRepository the registred new account repository
     */
    public AccountService(AppProperties appProperties, RegisteredUserRepository registredNewAccountRepository) {
        this.appProperties = appProperties;
    }

    private static boolean hasExpired(QrLoginChallenge challenge) {
        return !challenge.getExpiresAt().after(new Date());
    }

    private static final java.util.regex.Pattern PROFILE_PHONE = java.util.regex.Pattern.compile("^[+]?[0-9 ().\\-]{6,25}$");
    private static final java.util.regex.Pattern PROFILE_EMAIL = java.util.regex.Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    @Override
    @Transactional
    public Account updateProfile(String tenant, String userName, eu.isygoit.dto.data.AccountDto request) {
        Account account = findByTenantAndUserName(tenant, userName);
        if (account == null) {
            throw new eu.isygoit.exception.ObjectNotFoundException("account " + userName);
        }

        // ---- account level ----
        if (request.getEmail() != null) {
            String email = request.getEmail().trim();
            if (!PROFILE_EMAIL.matcher(email).matches()) {
                throw new eu.isygoit.exception.BadRequestException("Invalid email address");
            }
            account.setEmail(email);
        }
        if (request.getPhoneNumber() != null) {
            String phone = request.getPhoneNumber().trim();
            if (!phone.isEmpty() && !PROFILE_PHONE.matcher(phone).matches()) {
                throw new eu.isygoit.exception.BadRequestException("Invalid phone number");
            }
            account.setPhoneNumber(phone.isEmpty() ? null : phone);
        }
        if (request.getLanguage() != null) {
            account.setLanguage(request.getLanguage());
        }

        // ---- details ----
        AccountDetails details = account.getAccountDetails();
        if (details == null) {
            details = AccountDetails.builder().build();
            account.setAccountDetails(details);
        }
        eu.isygoit.dto.data.AccountDetailsDto sent = request.getAccountDetails();
        if (sent != null) {
            if (sent.getFirstName() != null) {
                details.setFirstName(requireName(sent.getFirstName(), "first name"));
            }
            if (sent.getLastName() != null) {
                details.setLastName(requireName(sent.getLastName(), "last name"));
            }
            if (sent.getCountry() != null) {
                details.setCountry(blankToNull(sent.getCountry()));
            }
            applyAddress(details, sent.getAddress());
            applyContacts(details, sent.getContacts());
        }

        try {
            return repository().saveAndFlush(account);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new eu.isygoit.exception.EmailAlreadyUsedException(
                    "The email or another profile value is already used: " + e.getMostSpecificCause().getMessage());
        }
    }

    private static String requireName(String value, String label) {
        String name = value.trim();
        if (name.length() < 2 || name.length() > 50) {
            throw new eu.isygoit.exception.BadRequestException("The " + label + " must have 2 to 50 characters");
        }
        return name;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * Each address field is replaced by what was sent (empty clears it); coordinates and the composed address lines
     * are not user input and stay as they are.
     */
    private static void applyAddress(AccountDetails details, eu.isygoit.dto.AddressDto sent) {
        if (sent == null) {
            return;
        }
        eu.isygoit.model.AccountAddress address = details.getAddress();
        if (address == null) {
            address = new eu.isygoit.model.AccountAddress();
            details.setAddress(address);
        }
        address.setStreet(blankToNull(sent.getStreet()));
        address.setZipCode(blankToNull(sent.getZipCode()));
        address.setCity(blankToNull(sent.getCity()));
        address.setState(blankToNull(sent.getState()));
        address.setCountry(blankToNull(sent.getCountry()));
        address.setAdditionalInfo(blankToNull(sent.getAdditionalInfo()));
    }

    /**
     * Replaces the contacts by the list sent; entries without a type or a value are dropped and the contacts that
     * disappear are deleted (orphan removal).
     */
    private static void applyContacts(AccountDetails details, java.util.List<eu.isygoit.dto.ContactDto> sent) {
        if (sent == null) {
            return;
        }
        java.util.List<eu.isygoit.model.AccountContact> contacts = details.getContacts();
        if (contacts == null) {
            contacts = new java.util.ArrayList<>();
            details.setContacts(contacts);
        }
        contacts.clear();
        for (eu.isygoit.dto.ContactDto dto : sent) {
            String value = blankToNull(dto.getValue());
            if (dto.getType() != null && value != null) {
                eu.isygoit.model.AccountContact contact = new eu.isygoit.model.AccountContact();
                contact.setType(dto.getType());
                contact.setValue(value);
                contacts.add(contact);
            }
        }
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
                return UserContext.builder().authTypeMode(IEnumAuth.Types.QRC)
                        .qrChallengeId(createQrLoginChallenge(account.getTenant(), account.getCode()))
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
    public String createQrLoginChallenge(String tenant, String userName) {
        Date createdAt = new Date();
        qrLoginChallengeRepository.deleteByExpiresAtBefore(createdAt);

        byte[] challengeBytes = new byte[32];
        QR_CHALLENGE_RANDOM.nextBytes(challengeBytes);
        String challengeId = Base64.getUrlEncoder().withoutPadding().encodeToString(challengeBytes);
        QrLoginChallenge challenge = QrLoginChallenge.builder()
                .challengeId(challengeId)
                .tenant(tenant.trim().toLowerCase(Locale.ROOT))
                .username(userName.trim().toLowerCase(Locale.ROOT))
                .status(QrLoginStatus.PENDING)
                .expiresAt(new Date(createdAt.getTime() + QR_CHALLENGE_TTL_MILLIS))
                .build();
        qrLoginChallengeRepository.save(challenge);
        return challengeId;
    }

    @Override
    public QrLoginStatus getQrLoginChallengeStatus(String challengeId) {
        Optional<QrLoginChallenge> optional = qrLoginChallengeRepository.findByChallengeId(challengeId);
        if (optional.isEmpty()) {
            return QrLoginStatus.EXPIRED;
        }

        QrLoginChallenge challenge = optional.get();
        if (hasExpired(challenge) && challenge.getStatus() != QrLoginStatus.COMPLETED) {
            challenge.setStatus(QrLoginStatus.EXPIRED);
            qrLoginChallengeRepository.save(challenge);
        }
        return challenge.getStatus();
    }

    @Override
    public QrLoginStatus approveQrLoginChallenge(String challengeId, String scannerTenant, String scannerUser) {
        Optional<QrLoginChallenge> optional = qrLoginChallengeRepository.lockByChallengeId(challengeId);
        if (optional.isEmpty()) {
            return QrLoginStatus.EXPIRED;
        }

        QrLoginChallenge challenge = optional.get();
        if (hasExpired(challenge) && challenge.getStatus() != QrLoginStatus.COMPLETED) {
            challenge.setStatus(QrLoginStatus.EXPIRED);
            qrLoginChallengeRepository.save(challenge);
            return QrLoginStatus.EXPIRED;
        }
        if (!challenge.getTenant().equalsIgnoreCase(scannerTenant)
                || !challenge.getUsername().equalsIgnoreCase(scannerUser)) {
            return QrLoginStatus.EXPIRED;
        }
        if (challenge.getStatus() == QrLoginStatus.PENDING) {
            challenge.setStatus(QrLoginStatus.APPROVED);
            challenge.setApprovedTenant(scannerTenant);
            challenge.setApprovedBy(scannerUser);
            qrLoginChallengeRepository.save(challenge);
        }
        return challenge.getStatus();
    }

    @Override
    public Optional<AuthenticationRequestDto> completeQrLoginChallenge(String challengeId) {
        Optional<QrLoginChallenge> optional = qrLoginChallengeRepository.lockByChallengeId(challengeId);
        if (optional.isEmpty()) {
            return Optional.empty();
        }

        QrLoginChallenge challenge = optional.get();
        if (challenge.getStatus() != QrLoginStatus.APPROVED || hasExpired(challenge)) {
            if (hasExpired(challenge) && challenge.getStatus() != QrLoginStatus.COMPLETED) {
                challenge.setStatus(QrLoginStatus.EXPIRED);
                qrLoginChallengeRepository.save(challenge);
            }
            return Optional.empty();
        }

        Account account = findByTenantAndUserName(challenge.getTenant(), challenge.getUsername());
        if (account == null) {
            challenge.setStatus(QrLoginStatus.EXPIRED);
            qrLoginChallengeRepository.save(challenge);
            return Optional.empty();
        }

        ResponseEntity<TokenResponseDto> tokenResponse;
        try {
            tokenResponse = kmsTokenService.buildToken(account.getTenant(),
                    Set.of(IEnumAuth.Types.QRC.meaning()),
                    IEnumToken.Types.QRC,
                    TokenRequestDto.builder()
                            .subject(account.getCode())
                            .claims(Map.of(JwtConstants.JWT_SENDER_TENANT, account.getTenant(),
                                    JwtConstants.JWT_LOG_APP, IEnumAuth.Types.QRC.meaning(),
                                    JwtConstants.JWT_SENDER_USER, account.getCode()))
                            .build());
        } catch (Exception exception) {
            throw new RemoteCallFailedException(exception);
        }
        if (!tokenResponse.getStatusCode().is2xxSuccessful() || tokenResponse.getBody() == null
                || tokenResponse.getBody().getToken() == null || tokenResponse.getBody().getToken().isBlank()) {
            throw new AccountAuthenticationException("QR sign-in token could not be issued");
        }

        challenge.setStatus(QrLoginStatus.COMPLETED);
        qrLoginChallengeRepository.save(challenge);
        return Optional.of(AuthenticationRequestDto.builder()
                .tenant(account.getTenant())
                .application(AuthConstants.DEFAULT_APPLICATION)
                .userName(account.getCode())
                .password(tokenResponse.getBody().getToken())
                .authType(IEnumAuth.Types.QRC)
                .build());
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
    public Optional<Account> findByEmail(String tenant, String email) {
        return accountRepository.findByTenantIgnoreCaseAndEmailIgnoreCase(tenant, email);
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

    @Override
    public boolean resendCreationEmail(String tenant, Long id) {
        try {
            Optional<Account> optionalAccount = this.findById(tenant, id);
            if (optionalAccount.isPresent()) {
                Account account = optionalAccount.get();
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
    public Account afterCreate(String tenant, Account account) {
        try {
            Optional<Tenant> accountTenant = tenantService.findByName(account.getTenant());
            ResponseEntity<Integer> result = kmsPasswordService.generatePwd(
                    GeneratePwdRequestDto.builder()
                            .tenant(account.getTenant())
                            .tenantUrl(accountTenant.map(Tenant::getUrl).orElse(null))
                            .email(account.getEmail())
                            .userName(account.getCode())
                            .fullName(account.getFullName())
                            .build());
            if (result.getStatusCode().is2xxSuccessful() && result.hasBody()) {
                return super.afterCreate(account);
            }
        } catch (Exception e) {
            log.error("Remote feign call failed : ", e);
            //throw new RemoteCallFailedException(e);
        }
        return super.afterCreate(account);
    }

    @Override
    public AppNextCode initCodeGenerator() {
        return AppNextCode.builder()
                .tenant(TenantConstants.SUPER_TENANT_NAME)
                .entity(Account.class.getSimpleName())
                .attribute(SchemaColumnConstantName.C_CODE)
                .prefix("ACT")
                .valueLength(6L)
                .codeValue(1L)
                .increment(1)
                .build();
    }
}

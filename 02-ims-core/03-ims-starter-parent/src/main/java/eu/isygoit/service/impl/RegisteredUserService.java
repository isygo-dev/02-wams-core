package eu.isygoit.service.impl;

import eu.isygoit.annotation.InjectCodeGen;
import eu.isygoit.annotation.InjectCodeGenKms;
import eu.isygoit.annotation.InjectRepository;
import eu.isygoit.com.rest.service.tenancy.CrudTenantService;
import eu.isygoit.config.AppProperties;
import eu.isygoit.dto.request.CreateAccountFromRegisteredRequestDto;
import eu.isygoit.enums.IEnumRegistrationStatus;
import eu.isygoit.exception.RegisteredUserNotFoundException;
import eu.isygoit.model.Account;
import eu.isygoit.model.AccountDetails;
import eu.isygoit.model.RegisteredUser;
import eu.isygoit.model.Tenant;
import eu.isygoit.remote.kms.KmsIncrementalKeyService;
import eu.isygoit.repository.RegisteredUserRepository;
import eu.isygoit.service.IRegisteredUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The type Account service.
 */
@Slf4j
@Service
@Transactional
@InjectCodeGen(value = NextCodeService.class)
@InjectCodeGenKms(value = KmsIncrementalKeyService.class)
@InjectRepository(value = RegisteredUserRepository.class)
public class RegisteredUserService extends CrudTenantService<Long, RegisteredUser, RegisteredUserRepository>
        implements IRegisteredUserService {

    private final AppProperties appProperties;

    @Autowired
    private AccountService accountService;

    @Autowired
    private TenantService tenantService;

    public RegisteredUserService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Transactional
    @Override
    public Account createAccountFromRegistered(String senderTenant, CreateAccountFromRegisteredRequestDto request) {
        RegisteredUser registeredUser = repository().findByEmail(request.getEmail())
                .orElseThrow(() -> new RegisteredUserNotFoundException("Registered user not found with email: " + request.getEmail()));

        if (request.getTenantInfo() != null) {
            Tenant newTenant = Tenant.builder()
                    .name(registeredUser.getOrganisation())
                    .email(registeredUser.getEmail())
                    .phone(registeredUser.getPhoneNumber())
                    .industry(request.getTenantInfo().getIndustry())
                    .url(request.getTenantInfo().getUrl())
                    .description(request.getTenantInfo().getDescription())
                    .adminStatus(request.getTenantInfo().getAdminStatus())
                    .build();

            tenantService.create(senderTenant, newTenant);
        }

        if (request.getAccountInfo() != null) {
            Account newAccount = Account.builder()
                    .tenant(registeredUser.getOrganisation())
                    .accountType(request.getAccountInfo().getAccountType())
                    .email(registeredUser.getEmail())
                    .phoneNumber(registeredUser.getPhoneNumber())
                    .language(request.getAccountInfo().getLanguage())
                    .functionRole(request.getAccountInfo().getFunctionalRole())
                    .isAdmin(request.getAccountInfo().isAdmin())
                    .adminStatus(request.getAccountInfo().getAdminStatus())
                    .accountDetails(AccountDetails.builder()
                            .firstName(registeredUser.getFirstName())
                            .lastName(registeredUser.getLastName())
                            .build())
                    .build();

            registeredUser.setStatus(IEnumRegistrationStatus.Types.PROCESSED);
            this.update(senderTenant, registeredUser);

            return accountService.create(senderTenant, newAccount);
        } else {
            log.warn("AccountInfo is null in the request.");
            throw new RegisteredUserNotFoundException("AccountInfo is required to create an account from registered user.");
        }
    }
}

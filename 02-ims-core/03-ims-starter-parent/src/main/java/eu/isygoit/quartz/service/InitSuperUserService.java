package eu.isygoit.quartz.service;

import eu.isygoit.config.AppProperties;
import eu.isygoit.constants.AccountTypeConstants;
import eu.isygoit.constants.DomainConstants;
import eu.isygoit.enums.IEnumAccountSystemStatus;
import eu.isygoit.enums.IEnumAuth;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.enums.IEnumLanguage;
import eu.isygoit.model.*;
import eu.isygoit.repository.ApiPermissionRepository;
import eu.isygoit.service.IAccountService;
import eu.isygoit.service.IApplicationService;
import eu.isygoit.service.IDomainService;
import eu.isygoit.service.IRoleInfoService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;

/**
 * The type Init super user service.
 */
@Data
@Slf4j
@Service
public class InitSuperUserService extends AbstractJobService {

    private final AppProperties appProperties;

    @Autowired
    private IAccountService accountService;
    @Autowired
    private IDomainService domainService;
    @Autowired
    private IRoleInfoService roleInfoService;
    @Autowired
    private ApiPermissionRepository apiPermissionRepository;
    @Autowired
    private IApplicationService applicationService;

    @Override
    public void performJob(JobExecutionContext jobExecutionContext) {

        //Check default domain existence
        Domain defaultDomain = domainService.findByName(DomainConstants.DEFAULT_DOMAIN_NAME);
        if (defaultDomain == null) {
            defaultDomain = domainService.create(Domain.builder()
                    .domain(DomainConstants.SUPER_DOMAIN_NAME)
                    .name(DomainConstants.DEFAULT_DOMAIN_NAME)
                    .description(DomainConstants.DEFAULT_DOMAIN_NAME)
                    .adminStatus(IEnumEnabledBinaryStatus.Types.ENABLED)
                    .build());
        }

        //Check super domain existence
        Domain superDomain = domainService.findByName(DomainConstants.SUPER_DOMAIN_NAME);
        if (superDomain == null) {
            superDomain = domainService.create(Domain.builder()
                    .domain(DomainConstants.SUPER_DOMAIN_NAME)
                    .name(DomainConstants.SUPER_DOMAIN_NAME)
                    .description(DomainConstants.SUPER_DOMAIN_NAME)
                    .adminStatus(IEnumEnabledBinaryStatus.Types.ENABLED)
                    .build());
        }

        //Check sysadmin application existence
        Application application = applicationService.findByName("webapp-sysadmin");
        if (application == null) {
            application = applicationService.create(Application.builder()
                    .domain(DomainConstants.SUPER_DOMAIN_NAME)
                    .title("System administration")
                    .category("PRM Store")
                    .name("webapp-sysadmin")
                    .description("System administration tool")
                    .url("https://fe-sysadmin.dev.prm.easygoit.eu")
                    .order(1)
                    .build());
        }

        //Check super role existence
        RoleInfo superAdmin = roleInfoService.findByName(AccountTypeConstants.SUPER_ADMIN);
        if (superAdmin == null) {
            superAdmin = roleInfoService.create(RoleInfo.builder()
                    .domain(DomainConstants.SUPER_DOMAIN_NAME)
                    .name(AccountTypeConstants.SUPER_ADMIN)
                    .description(AccountTypeConstants.SUPER_ADMIN)
                    .permissions(apiPermissionRepository.findAll())
                    .allowedTools(Arrays.asList(application))
                    .build());
        }

        //Check domain admin role existence
        RoleInfo domainAdmin = roleInfoService.findByName(AccountTypeConstants.DOMAIN_ADMIN);
        if (superAdmin == null) {
            superAdmin = roleInfoService.create(RoleInfo.builder()
                    .domain(DomainConstants.SUPER_DOMAIN_NAME)
                    .name(AccountTypeConstants.DOMAIN_ADMIN)
                    .description(AccountTypeConstants.DOMAIN_ADMIN)
                    .permissions(apiPermissionRepository.findAll())
                    .allowedTools(Arrays.asList(application))
                    .build());
        }

        //Check super user existence
        Account superUser = accountService.findByDomainAndUserName(DomainConstants.SUPER_DOMAIN_NAME, "root");
        if (superUser == null) {
            superUser = accountService.create(Account.builder()
                    .domain(DomainConstants.SUPER_DOMAIN_NAME)
                    .code("root")
                    .email("s.mbarki@isygoit.eu")
                    .language(IEnumLanguage.Types.EN)
                    .phoneNumber("0021653579452")
                    .systemStatus(IEnumAccountSystemStatus.Types.IDLE)
                    .adminStatus(IEnumEnabledBinaryStatus.Types.ENABLED)
                    .authType(IEnumAuth.Types.OTP)
                    .accountType(AccountTypeConstants.SUPER_ADMIN)
                    .functionRole(AccountTypeConstants.SUPER_ADMIN)
                    .isAdmin(true)
                    .accountDetails(AccountDetails.builder()
                            .firstName("Root")
                            .lastName("@SuperDomain")
                            .build())
                    .roleInfo(Arrays.asList(superAdmin))
                    .build());
        }
    }
}

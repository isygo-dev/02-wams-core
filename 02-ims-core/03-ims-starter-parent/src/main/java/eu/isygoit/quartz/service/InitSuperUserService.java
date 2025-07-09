package eu.isygoit.quartz.service;

import eu.isygoit.config.AppProperties;
import eu.isygoit.constants.AccountTypeConstants;
import eu.isygoit.constants.TenantConstants;
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
    private IDomainService tenantService;
    @Autowired
    private IRoleInfoService roleInfoService;
    @Autowired
    private ApiPermissionRepository apiPermissionRepository;
    @Autowired
    private IApplicationService applicationService;

    @Override
    public void performJob(JobExecutionContext jobExecutionContext) {

        //Check default tenant existence
        Domain defaultDomain = tenantService.findByName(TenantConstants.DEFAULT_TENANT_NAME);
        if (defaultDomain == null) {
            defaultDomain = tenantService.create(TenantConstants.SUPER_TENANT_NAME,
                    Domain.builder()
                            .tenant(TenantConstants.SUPER_TENANT_NAME)
                            .name(TenantConstants.DEFAULT_TENANT_NAME)
                            .description(TenantConstants.DEFAULT_TENANT_NAME)
                            .adminStatus(IEnumEnabledBinaryStatus.Types.ENABLED)
                            .build());
        }

        //Check super tenant existence
        Domain superDomain = tenantService.findByName(TenantConstants.SUPER_TENANT_NAME);
        if (superDomain == null) {
            superDomain = tenantService.create(TenantConstants.SUPER_TENANT_NAME,
                    Domain.builder()
                            .tenant(TenantConstants.SUPER_TENANT_NAME)
                            .name(TenantConstants.SUPER_TENANT_NAME)
                            .description(TenantConstants.SUPER_TENANT_NAME)
                            .adminStatus(IEnumEnabledBinaryStatus.Types.ENABLED)
                            .build());
        }

        //Check sysadmin application existence
        Application application = applicationService.findByName("webapp-sysadmin");
        if (application == null) {
            application = applicationService.create(TenantConstants.SUPER_TENANT_NAME,
                    Application.builder()
                            .tenant(TenantConstants.SUPER_TENANT_NAME)
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
            superAdmin = roleInfoService.create(AccountTypeConstants.SUPER_ADMIN,
                    RoleInfo.builder()
                            .tenant(TenantConstants.SUPER_TENANT_NAME)
                            .name(AccountTypeConstants.SUPER_ADMIN)
                            .description(AccountTypeConstants.SUPER_ADMIN)
                            .permissions(apiPermissionRepository.findAll())
                            .allowedTools(Arrays.asList(application))
                            .build());
        }

        //Check tenant admin role existence
        RoleInfo tenantAdmin = roleInfoService.findByName(AccountTypeConstants.TENANT_ADMIN);
        if (superAdmin == null) {
            superAdmin = roleInfoService.create(AccountTypeConstants.TENANT_ADMIN,
                    RoleInfo.builder()
                            .tenant(TenantConstants.SUPER_TENANT_NAME)
                            .name(AccountTypeConstants.TENANT_ADMIN)
                            .description(AccountTypeConstants.TENANT_ADMIN)
                            .permissions(apiPermissionRepository.findAll())
                            .allowedTools(Arrays.asList(application))
                            .build());
        }

        //Check super user existence
        Account superUser = accountService.findByTenantAndUserName(TenantConstants.SUPER_TENANT_NAME, "root");
        if (superUser == null) {
            superUser = accountService.create(TenantConstants.SUPER_TENANT_NAME,
                    Account.builder()
                            .tenant(TenantConstants.SUPER_TENANT_NAME)
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

package eu.isygoit.service.impl;

import eu.isygoit.annotation.InjectRepository;
import eu.isygoit.com.rest.service.CodeAssignableService;
import eu.isygoit.com.rest.service.tenancy.CodeAssignableTenantService;
import eu.isygoit.com.rest.service.CrudService;
import eu.isygoit.com.rest.service.tenancy.CrudTenantService;
import eu.isygoit.constants.AppParameterConstants;
import eu.isygoit.constants.TenantConstants;
import eu.isygoit.model.AppParameter;
import eu.isygoit.repository.AppParameterRepository;
import eu.isygoit.service.IAppParameterService;
import eu.isygoit.service.IDomainService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * The type App parameter service.
 */
@Slf4j
@Service
@Transactional
@InjectRepository(value = AppParameterRepository.class)
public class AppParameterService extends CrudTenantService<Long, AppParameter, AppParameterRepository> implements IAppParameterService {

    @Autowired
    private IDomainService tenantService;

    @Override
    public String getValueByTenantAndName(String tenant, String name, boolean allowDefault, String defaultValue) {
        Optional<AppParameter> optional = repository().findByTenantIgnoreCaseAndNameIgnoreCase(tenant, name);
        if (!StringUtils.hasText(defaultValue)) {
            defaultValue = "NA";
        }
        if (optional.isPresent() && StringUtils.hasText(optional.get().getValue())) {
            return optional.get().getValue();
        } else if (allowDefault) {
            optional = repository().findByTenantIgnoreCaseAndNameIgnoreCase(TenantConstants.DEFAULT_TENANT_NAME, name);
            if (optional.isPresent() && StringUtils.hasText(optional.get().getValue())) {
                return optional.get().getValue();
            } else {
                this.create(tenant, AppParameter.builder()
                        .tenant(TenantConstants.DEFAULT_TENANT_NAME)
                        .name(name)
                        .description(name)
                        .value(defaultValue)
                        .build());
            }
        } else {
            this.create(tenant, AppParameter.builder()
                    .tenant(tenant)
                    .name(name)
                    .description(name)
                    .value(defaultValue)
                    .build());
        }

        return defaultValue;
    }

    @Override
    public String getTechnicalAdminEmail() {
        String techAdminEmail = this.getValueByTenantAndName(TenantConstants.SUPER_TENANT_NAME,
                AppParameterConstants.TECHNICAL_ADMIN_EMAIL,
                false,
                "NA");
        if (!StringUtils.hasText(techAdminEmail)) {
            techAdminEmail = tenantService.findByName(TenantConstants.SUPER_TENANT_NAME).getEmail();
        }
        return techAdminEmail;
    }
}

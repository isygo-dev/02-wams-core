package eu.isygoit.service.impl;

import eu.isygoit.annotation.ServRepo;
import eu.isygoit.com.rest.service.CrudService;
import eu.isygoit.constants.AppParameterConstants;
import eu.isygoit.constants.DomainConstants;
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
@ServRepo(value = AppParameterRepository.class)
public class AppParameterService extends CrudService<Long, AppParameter, AppParameterRepository> implements IAppParameterService {

    @Autowired
    private IDomainService domainService;

    @Override
    public String getValueByDomainAndName(String domain, String name, boolean allowDefault, String defaultValue) {
        Optional<AppParameter> optional = repository().findByDomainIgnoreCaseAndNameIgnoreCase(domain, name);
        if (!StringUtils.hasText(defaultValue)) {
            defaultValue = "NA";
        }
        if (optional.isPresent() && StringUtils.hasText(optional.get().getValue())) {
            return optional.get().getValue();
        } else if (allowDefault) {
            optional = repository().findByDomainIgnoreCaseAndNameIgnoreCase(DomainConstants.DEFAULT_DOMAIN_NAME, name);
            if (optional.isPresent() && StringUtils.hasText(optional.get().getValue())) {
                return optional.get().getValue();
            } else {
                this.create(AppParameter.builder()
                        .domain(DomainConstants.DEFAULT_DOMAIN_NAME)
                        .name(name)
                        .description(name)
                        .value(defaultValue)
                        .build());
            }
        } else {
            this.create(AppParameter.builder()
                    .domain(domain)
                    .name(name)
                    .description(name)
                    .value(defaultValue)
                    .build());
        }

        return defaultValue;
    }

    @Override
    public String getTechnicalAdminEmail() {
        String techAdminEmail = this.getValueByDomainAndName(DomainConstants.SUPER_DOMAIN_NAME,
                AppParameterConstants.TECHNICAL_ADMIN_EMAIL,
                false,
                "NA");
        if (!StringUtils.hasText(techAdminEmail)) {
            techAdminEmail = domainService.findByName(DomainConstants.SUPER_DOMAIN_NAME).getEmail();
        }
        return techAdminEmail;
    }
}

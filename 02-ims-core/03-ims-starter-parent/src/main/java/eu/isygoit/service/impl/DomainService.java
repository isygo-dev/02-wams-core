package eu.isygoit.service.impl;

import eu.isygoit.annotation.InjectCodeGen;
import eu.isygoit.annotation.InjectCodeGenKms;
import eu.isygoit.annotation.InjectRepository;
import eu.isygoit.com.rest.service.tenancy.ImageTenantService;
import eu.isygoit.config.AppProperties;
import eu.isygoit.constants.TenantConstants;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.exception.DomainNotFoundException;
import eu.isygoit.model.AppNextCode;
import eu.isygoit.model.Domain;
import eu.isygoit.model.schema.SchemaColumnConstantName;
import eu.isygoit.remote.kms.KmsIncrementalKeyService;
import eu.isygoit.repository.DomainRepository;
import eu.isygoit.service.IDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * The type Domain service.
 */
@Service
@Transactional
@InjectCodeGen(value = NextCodeService.class)
@InjectCodeGenKms(value = KmsIncrementalKeyService.class)
@InjectRepository(value = DomainRepository.class)
public class DomainService extends ImageTenantService<Long, Domain, DomainRepository> implements IDomainService {

    private final AppProperties appProperties;


    /**
     * Instantiates a new Domain service.
     *
     * @param appProperties the app properties
     */
    public DomainService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public List<String> getAllDomainNames(String tenant) {
        if (TenantConstants.SUPER_TENANT_NAME.equals(tenant)) {
            return repository().getAllNames(); //.findAll().stream().map(tenant -> tenant.getName()).toList();
        }

        return Arrays.asList(tenant);
    }

    @Override
    public Domain updateAdminStatus(Long id, IEnumEnabledBinaryStatus.Types newStatus) {
        repository().updateAdminStatusById(id, newStatus);
        return repository().findById(id).orElse(null);
    }

    @Override
    public String getImage(String tenantName) {
        Optional<Domain> tenantOptional = repository().findByNameIgnoreCase(tenantName);
        if (tenantOptional.isPresent()) {
            return tenantOptional.get().getImagePath();
        }
        return "";
    }

    @Override
    public Long findDomainIdbyDomainName(String name) {
        Optional<Domain> tenant = repository().findByNameIgnoreCase(name);
        if (tenant.isPresent()) {
            return tenant.get().getId();
        }
        return null;
    }

    @Override
    public Domain findByName(String name) {
        Optional<Domain> optional = repository().findByNameIgnoreCase(name);
        if (optional.isPresent()) {
            return optional.get();
        }
        return null;
    }

    @Override
    public boolean isEnabled(String tenant) {
        return repository().getAdminStatus(tenant) == IEnumEnabledBinaryStatus.Types.ENABLED;
    }

    @Override
    public Domain updateSocialLink(String tenant, Long id, String social, String link) {
        Optional<Domain> optional = repository().findById(id);
        if (!optional.isPresent()) {
            throw new DomainNotFoundException("with id " + id);
        }

        Domain domain = optional.get();
        switch (social) {
            case "lnk_facebook": {
                domain.setLnk_facebook(link);
            }
            break;
            case "lnk_linkedin": {
                domain.setLnk_linkedin(link);
            }
            break;
            case "lnk_xing": {
                domain.setLnk_xing(link);
            }
            break;
        }

        return this.update(tenant, domain);
    }

    @Override
    public AppNextCode initCodeGenerator() {
        return AppNextCode.builder()
                .tenant(TenantConstants.DEFAULT_TENANT_NAME)
                .entity(Domain.class.getSimpleName())
                .attribute(SchemaColumnConstantName.C_CODE)
                .prefix("DOM")
                .valueLength(6L)
                .codeValue(1L)
                .increment(1)
                .build();
    }

    @Override
    protected String getUploadDirectory() {
        return this.appProperties.getUploadDirectory();
    }
}

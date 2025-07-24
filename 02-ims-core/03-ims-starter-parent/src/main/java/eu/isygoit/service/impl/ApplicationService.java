package eu.isygoit.service.impl;

import eu.isygoit.annotation.InjectCodeGen;
import eu.isygoit.annotation.InjectCodeGenKms;
import eu.isygoit.annotation.InjectRepository;
import eu.isygoit.com.rest.service.tenancy.ImageTenantService;
import eu.isygoit.config.AppProperties;
import eu.isygoit.constants.TenantConstants;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.model.AppNextCode;
import eu.isygoit.model.Application;
import eu.isygoit.model.schema.SchemaColumnConstantName;
import eu.isygoit.remote.kms.KmsIncrementalKeyService;
import eu.isygoit.repository.ApplicationRepository;
import eu.isygoit.service.IApplicationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The type Application service.
 */
@Service
@Transactional
@InjectCodeGen(value = NextCodeService.class)
@InjectCodeGenKms(value = KmsIncrementalKeyService.class)
@InjectRepository(value = ApplicationRepository.class)
public class ApplicationService extends ImageTenantService<Long, Application, ApplicationRepository>
        implements IApplicationService {

    private final AppProperties appProperties;

    /**
     * Instantiates a new Application service.
     *
     * @param appProperties the app properties
     */
    public ApplicationService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public AppNextCode initCodeGenerator() {
        return AppNextCode.builder()
                .tenant(TenantConstants.DEFAULT_TENANT_NAME)
                .entity(Application.class.getSimpleName())
                .attribute(SchemaColumnConstantName.C_CODE)
                .prefix("APP")
                .valueLength(6L)
                .codeValue(1L)
                .increment(1)
                .build();
    }

    @Override
    protected String getUploadDirectory() {
        return this.appProperties.getUploadDirectory();
    }

    @Override
    public Application updateStatus(Long id, IEnumEnabledBinaryStatus.Types newStatus) {
        repository().updateAdminStatusById(id, newStatus);
        return repository().findById(id).orElse(null);
    }

    @Override
    public Application findByName(String name) {
        return repository().findByNameIgnoreCase(name);
    }
}

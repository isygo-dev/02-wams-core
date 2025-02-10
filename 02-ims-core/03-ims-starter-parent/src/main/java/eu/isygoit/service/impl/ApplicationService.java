package eu.isygoit.service.impl;

import eu.isygoit.annotation.CodeGenKms;
import eu.isygoit.annotation.CodeGenLocal;
import eu.isygoit.annotation.SrvRepo;
import eu.isygoit.app.ApplicationContextService;
import eu.isygoit.com.rest.service.impl.ImageService;
import eu.isygoit.config.AppProperties;
import eu.isygoit.constants.DomainConstants;
import eu.isygoit.enums.IEnumBinaryStatus;
import eu.isygoit.model.AppNextCode;
import eu.isygoit.model.Application;
import eu.isygoit.model.extendable.NextCodeModel;
import eu.isygoit.model.schema.SchemaColumnConstantName;
import eu.isygoit.remote.kms.KmsIncrementalKeyService;
import eu.isygoit.repository.ApplicationRepository;
import eu.isygoit.service.IApplicationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * The type Application service.
 */
@Service
@Transactional
@CodeGenLocal(value = NextCodeService.class)
@CodeGenKms(value = KmsIncrementalKeyService.class)
@SrvRepo(value = ApplicationRepository.class)
public class ApplicationService extends ImageService<Long, Application, ApplicationRepository>
        implements IApplicationService {

    private final ApplicationContextService applicationContextService;
    @Override
    protected ApplicationContextService getApplicationContextServiceInstance() {
        return applicationContextService;
    }

    private final AppProperties appProperties;

    /**
     * Instantiates a new Application service.
     *
     * @param appProperties the app properties
     */
    public ApplicationService(ApplicationContextService applicationContextService, AppProperties appProperties) {
        this.applicationContextService = applicationContextService;
        this.appProperties = appProperties;
    }

    @Override
    public Optional<NextCodeModel> initCodeGenerator() {
        return Optional.ofNullable(AppNextCode.builder()
                .domain(DomainConstants.DEFAULT_DOMAIN_NAME)
                .entity(Application.class.getSimpleName())
                .attribute(SchemaColumnConstantName.C_CODE)
                .prefix("APP")
                .valueLength(6L)
                .value(1L)
                .increment(1)
                .build());
    }

    @Override
    protected String getUploadDirectory() {
        return this.appProperties.getUploadDirectory();
    }

    @Override
    public Application updateStatus(Long id, IEnumBinaryStatus.Types newStatus) {
        repository().updateAdminStatusById(id, newStatus);
        return repository().findById(id).orElse(null);
    }

    @Override
    public Optional<Application> getByName(String name) {
        return repository().findByNameIgnoreCase(name);
    }
}

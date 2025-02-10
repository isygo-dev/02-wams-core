package eu.isygoit.service.impl;

import eu.isygoit.annotation.CodeGenLocal;
import eu.isygoit.annotation.SrvRepo;
import eu.isygoit.app.ApplicationContextService;
import eu.isygoit.com.rest.service.impl.CodifiableService;
import eu.isygoit.constants.DomainConstants;
import eu.isygoit.model.AppNextCode;
import eu.isygoit.model.PasswordConfig;
import eu.isygoit.model.extendable.NextCodeModel;
import eu.isygoit.model.schema.SchemaColumnConstantName;
import eu.isygoit.repository.PasswordConfigRepository;
import eu.isygoit.service.IPasswordConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * The type Password config service.
 */
@Service
@Transactional
@CodeGenLocal(value = NextCodeService.class)
@SrvRepo(value = PasswordConfigRepository.class)
public class PasswordConfigService extends CodifiableService<Long, PasswordConfig, PasswordConfigRepository> implements IPasswordConfigService {

    private final ApplicationContextService applicationContextService;

    public PasswordConfigService(ApplicationContextService applicationContextService) {
        this.applicationContextService = applicationContextService;
    }

    @Override
    protected ApplicationContextService getApplicationContextServiceInstance() {
        return applicationContextService;
    }

    @Override
    public Optional<NextCodeModel> initCodeGenerator() {
        return Optional.ofNullable(AppNextCode.builder()
                .domain(DomainConstants.DEFAULT_DOMAIN_NAME)
                .entity(PasswordConfig.class.getSimpleName())
                .attribute(SchemaColumnConstantName.C_CODE)
                .prefix("PWD")
                .valueLength(6L)
                .value(1L)
                .increment(1)
                .build());
    }
}

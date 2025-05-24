package eu.isygoit.service.impl;

import eu.isygoit.annotation.CodeGenLocal;
import eu.isygoit.annotation.ServRepo;
import eu.isygoit.com.rest.service.CodeAssignableService;
import eu.isygoit.constants.DomainConstants;
import eu.isygoit.model.AppNextCode;
import eu.isygoit.model.PasswordConfig;
import eu.isygoit.model.schema.SchemaColumnConstantName;
import eu.isygoit.repository.PasswordConfigRepository;
import eu.isygoit.service.IPasswordConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The type Password config service.
 */
@Service
@Transactional
@CodeGenLocal(value = NextCodeService.class)
@ServRepo(value = PasswordConfigRepository.class)
public class PasswordConfigService extends CodeAssignableService<Long, PasswordConfig, PasswordConfigRepository> implements IPasswordConfigService {

    @Override
    public AppNextCode initCodeGenerator() {
        return AppNextCode.builder()
                .domain(DomainConstants.DEFAULT_DOMAIN_NAME)
                .entity(PasswordConfig.class.getSimpleName())
                .attribute(SchemaColumnConstantName.C_CODE)
                .prefix("PWD")
                .valueLength(6L)
                .value(1L)
                .increment(1)
                .build();
    }
}

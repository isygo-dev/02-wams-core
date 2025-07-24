package eu.isygoit.service.impl;

import eu.isygoit.annotation.InjectCodeGen;
import eu.isygoit.annotation.InjectRepository;
import eu.isygoit.com.rest.service.tenancy.CodeAssignableTenantService;
import eu.isygoit.constants.TenantConstants;
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
@InjectCodeGen(value = NextCodeService.class)
@InjectRepository(value = PasswordConfigRepository.class)
public class PasswordConfigService extends CodeAssignableTenantService<Long, PasswordConfig, PasswordConfigRepository> implements IPasswordConfigService {

    @Override
    public AppNextCode initCodeGenerator() {
        return AppNextCode.builder()
                .tenant(TenantConstants.DEFAULT_TENANT_NAME)
                .entity(PasswordConfig.class.getSimpleName())
                .attribute(SchemaColumnConstantName.C_CODE)
                .prefix("PWD")
                .valueLength(6L)
                .codeValue(1L)
                .increment(1)
                .build();
    }
}

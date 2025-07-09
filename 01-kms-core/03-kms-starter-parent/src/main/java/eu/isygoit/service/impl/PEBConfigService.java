package eu.isygoit.service.impl;

import eu.isygoit.annotation.InjectCodeGen;
import eu.isygoit.annotation.InjectRepository;
import eu.isygoit.com.rest.service.tenancy.CodeAssignableTenantService;
import eu.isygoit.constants.TenantConstants;
import eu.isygoit.model.AppNextCode;
import eu.isygoit.model.PEBConfig;
import eu.isygoit.model.schema.SchemaColumnConstantName;
import eu.isygoit.repository.PEBConfigRepository;
import eu.isygoit.service.IPEBConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The type Peb config service.
 */
@Service
@Transactional
@InjectCodeGen(value = NextCodeService.class)
@InjectRepository(value = PEBConfigRepository.class)
public class PEBConfigService extends CodeAssignableTenantService<Long, PEBConfig, PEBConfigRepository> implements IPEBConfigService {

    @Override
    public AppNextCode initCodeGenerator() {
        return AppNextCode.builder()
                .tenant(TenantConstants.DEFAULT_TENANT_NAME)
                .entity(PEBConfig.class.getSimpleName())
                .attribute(SchemaColumnConstantName.C_CODE)
                .prefix("PEB")
                .valueLength(6L)
                .value(1L)
                .increment(1)
                .build();
    }
}

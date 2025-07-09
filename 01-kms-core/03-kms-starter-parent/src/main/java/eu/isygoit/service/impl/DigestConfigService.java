package eu.isygoit.service.impl;

import eu.isygoit.annotation.InjectCodeGen;
import eu.isygoit.annotation.InjectRepository;
import eu.isygoit.com.rest.service.tenancy.CodeAssignableTenantService;
import eu.isygoit.constants.TenantConstants;
import eu.isygoit.model.AppNextCode;
import eu.isygoit.model.DigestConfig;
import eu.isygoit.model.schema.SchemaColumnConstantName;
import eu.isygoit.repository.DigesterConfigRepository;
import eu.isygoit.service.IDigestConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The type Digest config service.
 */
@Service
@Transactional
@InjectCodeGen(value = NextCodeService.class)
@InjectRepository(value = DigesterConfigRepository.class)
public class DigestConfigService extends CodeAssignableTenantService<Long, DigestConfig, DigesterConfigRepository> implements IDigestConfigService {

    @Override
    public AppNextCode initCodeGenerator() {
        return AppNextCode.builder()
                .tenant(TenantConstants.DEFAULT_TENANT_NAME)
                .entity(DigestConfig.class.getSimpleName())
                .attribute(SchemaColumnConstantName.C_CODE)
                .prefix("DIG")
                .valueLength(6L)
                .value(1L)
                .increment(1)
                .build();
    }
}

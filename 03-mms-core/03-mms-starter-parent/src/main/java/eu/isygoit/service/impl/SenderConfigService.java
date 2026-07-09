package eu.isygoit.service.impl;

import eu.isygoit.annotation.InjectRepository;
import eu.isygoit.com.rest.service.tenancy.CodeAssignableTenantService;
import eu.isygoit.constants.TenantConstants;
import eu.isygoit.model.AppNextCode;
import eu.isygoit.model.SenderConfig;
import eu.isygoit.model.schema.SchemaColumnConstantName;
import eu.isygoit.repository.SenderConfigRepository;
import eu.isygoit.service.ISenderConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The type Sender config service.
 */
@Service
@Transactional
@InjectRepository(value = SenderConfigRepository.class)
public class SenderConfigService extends CodeAssignableTenantService<Long, SenderConfig, SenderConfigRepository> implements ISenderConfigService {

    @Override
    public AppNextCode initCodeGenerator() {
        return AppNextCode.builder()
                .tenant(TenantConstants.DEFAULT_TENANT_NAME)
                .entity(SenderConfig.class.getSimpleName())
                .attribute(SchemaColumnConstantName.C_CODE)
                .prefix("SCO")
                .valueLength(6L)
                .codeValue(1L)
                .increment(1)
                .build();
    }
}

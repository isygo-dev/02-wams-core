package eu.isygoit.service.impl;

import eu.isygoit.annotation.CodeGenLocal;
import eu.isygoit.annotation.SrvRepo;
import eu.isygoit.com.rest.service.CodeAssignableService;
import eu.isygoit.constants.DomainConstants;
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
@CodeGenLocal(value = NextCodeService.class)
@SrvRepo(value = PEBConfigRepository.class)
public class PEBConfigService extends CodeAssignableService<Long, PEBConfig, PEBConfigRepository> implements IPEBConfigService {

    @Override
    public AppNextCode initCodeGenerator() {
        return AppNextCode.builder()
                .domain(DomainConstants.DEFAULT_DOMAIN_NAME)
                .entity(PEBConfig.class.getSimpleName())
                .attribute(SchemaColumnConstantName.C_CODE)
                .prefix("PEB")
                .valueLength(6L)
                .value(1L)
                .increment(1)
                .build();
    }
}

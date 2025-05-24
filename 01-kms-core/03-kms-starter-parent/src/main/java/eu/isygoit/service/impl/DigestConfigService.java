package eu.isygoit.service.impl;

import eu.isygoit.annotation.CodeGenLocal;
import eu.isygoit.annotation.ServRepo;
import eu.isygoit.com.rest.service.CodeAssignableService;
import eu.isygoit.constants.DomainConstants;
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
@CodeGenLocal(value = NextCodeService.class)
@ServRepo(value = DigesterConfigRepository.class)
public class DigestConfigService extends CodeAssignableService<Long, DigestConfig, DigesterConfigRepository> implements IDigestConfigService {

    @Override
    public AppNextCode initCodeGenerator() {
        return AppNextCode.builder()
                .domain(DomainConstants.DEFAULT_DOMAIN_NAME)
                .entity(DigestConfig.class.getSimpleName())
                .attribute(SchemaColumnConstantName.C_CODE)
                .prefix("DIG")
                .valueLength(6L)
                .value(1L)
                .increment(1)
                .build();
    }
}

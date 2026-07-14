package eu.isygoit.service.impl;

import eu.isygoit.annotation.InjectCodeGen;
import eu.isygoit.annotation.InjectCodeGenKms;
import eu.isygoit.annotation.InjectRepository;
import eu.isygoit.com.rest.service.tenancy.CrudTenantService;
import eu.isygoit.config.AppProperties;
import eu.isygoit.model.*;
import eu.isygoit.remote.kms.KmsIncrementalKeyService;
import eu.isygoit.repository.RegistredUserRepository;
import eu.isygoit.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The type Account service.
 */
@Slf4j
@Service
@Transactional
@InjectCodeGen(value = NextCodeService.class)
@InjectCodeGenKms(value = KmsIncrementalKeyService.class)
@InjectRepository(value = RegistredUserRepository.class)
public class RegisteredUserService extends CrudTenantService<Long, RegisteredUser, RegistredUserRepository>
        implements IRegisteredUserService {

    private final AppProperties appProperties;

    public RegisteredUserService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }
}

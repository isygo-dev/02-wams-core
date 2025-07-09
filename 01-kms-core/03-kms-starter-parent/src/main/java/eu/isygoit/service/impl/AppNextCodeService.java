package eu.isygoit.service.impl;

import eu.isygoit.annotation.InjectRepository;
import eu.isygoit.com.rest.service.tenancy.CrudTenantService;
import eu.isygoit.model.AppNextCode;
import eu.isygoit.repository.AppNextCodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The type App next code service.
 */
@Service
@Transactional
@InjectRepository(value = AppNextCodeRepository.class)
public class AppNextCodeService extends CrudTenantService<Long, AppNextCode, AppNextCodeRepository> {

}

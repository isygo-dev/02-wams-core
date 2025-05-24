package eu.isygoit.service.impl;

import eu.isygoit.annotation.ServRepo;
import eu.isygoit.com.rest.service.CrudService;
import eu.isygoit.model.AppNextCode;
import eu.isygoit.repository.AppNextCodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The type App next code service.
 */
@Service
@Transactional
@ServRepo(value = AppNextCodeRepository.class)
public class AppNextCodeService extends CrudService<Long, AppNextCode, AppNextCodeRepository> {

}

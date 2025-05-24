package eu.isygoit.service.impl;

import eu.isygoit.annotation.ServRepo;
import eu.isygoit.com.rest.service.CrudService;
import eu.isygoit.model.SenderConfig;
import eu.isygoit.repository.SenderConfigRepository;
import eu.isygoit.service.ISenderConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The type Sender config service.
 */
@Service
@Transactional
@ServRepo(value = SenderConfigRepository.class)
public class SenderConfigService extends CrudService<Long, SenderConfig, SenderConfigRepository> implements ISenderConfigService {

}

package eu.isygoit.service.impl;

import eu.isygoit.annotation.ServRepo;
import eu.isygoit.com.rest.service.CrudService;
import eu.isygoit.model.AccountDetails;
import eu.isygoit.repository.AccountDetailsRepository;
import eu.isygoit.service.IAccountDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The type Account details service.
 */
@Service
@Transactional
@ServRepo(value = AccountDetailsRepository.class)
public class AccountDetailsService extends CrudService<Long, AccountDetails, AccountDetailsRepository> implements IAccountDetailsService {
}

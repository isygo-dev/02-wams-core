package eu.isygoit.service.impl;

import eu.isygoit.annotation.InjectRepository;
import eu.isygoit.com.rest.service.CodeAssignableService;
import eu.isygoit.com.rest.service.tenancy.CodeAssignableTenantService;
import eu.isygoit.com.rest.service.CrudService;
import eu.isygoit.com.rest.service.tenancy.CrudTenantService;
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
@InjectRepository(value = AccountDetailsRepository.class)
public class AccountDetailsService extends CrudService<Long, AccountDetails, AccountDetailsRepository> implements IAccountDetailsService {
}

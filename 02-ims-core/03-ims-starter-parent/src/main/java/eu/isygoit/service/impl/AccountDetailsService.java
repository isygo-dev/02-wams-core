package eu.isygoit.service.impl;

import eu.isygoit.annotation.SrvRepo;
import eu.isygoit.app.ApplicationContextService;
import eu.isygoit.com.rest.service.impl.CrudService;
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
@SrvRepo(value = AccountDetailsRepository.class)
public class AccountDetailsService extends CrudService<Long, AccountDetails, AccountDetailsRepository> implements IAccountDetailsService {

    private final ApplicationContextService applicationContextService;

    public AccountDetailsService(ApplicationContextService applicationContextService) {
        this.applicationContextService = applicationContextService;
    }

    @Override
    protected ApplicationContextService getApplicationContextServiceInstance() {
        return applicationContextService;
    }
}

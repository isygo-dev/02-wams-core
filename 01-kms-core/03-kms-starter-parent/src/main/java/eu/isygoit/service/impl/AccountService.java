package eu.isygoit.service.impl;

import eu.isygoit.annotation.InjectCodeGen;
import eu.isygoit.annotation.InjectRepository;
import eu.isygoit.com.rest.service.CodeAssignableService;
import eu.isygoit.com.rest.service.tenancy.CodeAssignableTenantService;
import eu.isygoit.com.rest.service.CrudService;
import eu.isygoit.com.rest.service.tenancy.CrudTenantService;
import eu.isygoit.model.Account;
import eu.isygoit.repository.AccountRepository;
import eu.isygoit.service.IAccountService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * The type Account service.
 */
@Service
@Transactional
@InjectCodeGen(value = NextCodeService.class)
@InjectRepository(value = AccountRepository.class)
public class AccountService extends CrudTenantService<Long, Account, AccountRepository> implements IAccountService {

    @Override
    public boolean checkIfExists(Account account, boolean createIfNotExists) {
        Optional<Account> optional = repository().findByTenantIgnoreCaseAndCodeIgnoreCase(account.getTenant(), account.getCode());
        if (optional.isPresent()) {
            Account existing = optional.get();
            existing.setEmail(account.getEmail());
            existing.setAdminStatus(account.getAdminStatus());
            existing.setSystemStatus(account.getSystemStatus());
            existing.setFullName(account.getFullName());
            this.update(existing.getTenant(), existing);
            return true;
        } else if (createIfNotExists) {
            //Create the account if not exists
            this.create(account.getTenant(), account);
            return true;
        }

        return false;
    }
}

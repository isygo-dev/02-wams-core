package eu.isygoit.service.impl;

import eu.isygoit.annotation.CodeGenLocal;
import eu.isygoit.annotation.SrvRepo;
import eu.isygoit.com.rest.service.CrudService;
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
@CodeGenLocal(value = NextCodeService.class)
@SrvRepo(value = AccountRepository.class)
public class AccountService extends CrudService<Long, Account, AccountRepository> implements IAccountService {

    @Override
    public boolean checkIfExists(Account account, boolean createIfNotExists) {
        Optional<Account> optional = repository().findByCodeIgnoreCase(account.getCode());
        if (optional.isPresent()) {
            Account existing = optional.get();
            existing.setEmail(account.getEmail());
            existing.setAdminStatus(account.getAdminStatus());
            existing.setSystemStatus(account.getSystemStatus());
            existing.setFullName(account.getFullName());
            this.update(existing);
            return true;
        } else if (createIfNotExists) {
            //Create the account if not exists
            this.create(account);
            return true;
        }

        return false;
    }
}

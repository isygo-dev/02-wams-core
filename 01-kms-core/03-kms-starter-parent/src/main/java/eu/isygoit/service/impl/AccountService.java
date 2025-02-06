package eu.isygoit.service.impl;

import eu.isygoit.annotation.CodeGenLocal;
import eu.isygoit.annotation.SrvRepo;
import eu.isygoit.com.rest.service.impl.CrudService;
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
    public Optional<Account> checkIfExists(Account account, boolean createIfNotExists) {
        return Optional.ofNullable(repository().findByCodeIgnoreCase(account.getCode())
                .map(existing -> {
                    existing.setEmail(account.getEmail());
                    existing.setAdminStatus(account.getAdminStatus());
                    existing.setSystemStatus(account.getSystemStatus());
                    existing.setFullName(account.getFullName());
                    return this.update(existing);
                }).orElseGet(() -> {
                    if (createIfNotExists) {
                        return this.create(account);
                    }
                    return null;
                }));
    }
}

package eu.isygoit.service.impl;

import eu.isygoit.annotation.InjectCodeGen;
import eu.isygoit.annotation.InjectRepository;
import eu.isygoit.com.rest.service.CodeAssignableService;
import eu.isygoit.com.rest.service.tenancy.CodeAssignableTenantService;
import eu.isygoit.com.rest.service.CrudService;
import eu.isygoit.com.rest.service.tenancy.CrudTenantService;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.model.Account;
import eu.isygoit.model.KmsDomain;
import eu.isygoit.repository.AccountRepository;
import eu.isygoit.repository.DomainRepository;
import eu.isygoit.service.IDomainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * The type Domain service.
 */
@Service
@Transactional
@InjectCodeGen(value = NextCodeService.class)
@InjectRepository(value = DomainRepository.class)
public class DomainService extends CrudService<Long, KmsDomain, DomainRepository> implements IDomainService {

    @Autowired
    private AccountRepository accountRepository;

    @Override
    public KmsDomain checkDomainIfExists(String tenantName, String tenantUrl, boolean createIfNotExists) {
        Optional<KmsDomain> optional = repository().findByNameIgnoreCase(tenantName);
        if (optional.isPresent()) {
            return optional.get();
        } else if (createIfNotExists) {
            //Create the tenant if not exists
            return this.create(KmsDomain.builder()
                    .name(tenantName)
                    .url(tenantUrl)
                    .description(tenantName)
                    .build());
        }

        return null;
    }

    @Override
    public KmsDomain findByNameIgnoreCase(String tenantName) {
        Optional<KmsDomain> optional = repository().findByNameIgnoreCase(tenantName);
        if (optional.isPresent()) {
            return optional.get();
        }

        return null;
    }

    @Override
    public Account checkAccountIfExists(String tenantName, String tenantUrl, String email, String userName, String fullName, boolean createIfNotExists) {
        //Check tenant if exists
        KmsDomain kmsDomain = this.checkDomainIfExists(tenantName, tenantUrl, createIfNotExists);
        if (kmsDomain == null) {
            return null;
        }

        //Check account if exists
        Optional<Account> optional = accountRepository.findByTenantIgnoreCaseAndCodeIgnoreCase(tenantName, userName);
        if (optional.isPresent()) {
            //Update account email if changed
            Account account = optional.get();
            if (StringUtils.hasText(email) && !account.getEmail().equals(email)) {
                account.setEmail(email);
                accountRepository.save(account);
            }
            return account;
        }

        //Create the account if not exists
        if (createIfNotExists) {
            return accountRepository.save(Account.builder()
                    .code(userName)
                    .email(email)
                    .tenant(tenantName)
                    .fullName(fullName)
                    .build());
        }
        return null;
    }

    @Override
    public boolean checkIfExists(KmsDomain kmsDomain, boolean createIfNotExists) {
        Optional<KmsDomain> optional = repository().findByNameIgnoreCase(kmsDomain.getName());
        if (optional.isPresent()) {
            //Update the tenant if not exists
            kmsDomain.setId(optional.get().getId());
            this.update(kmsDomain);
            return true;
        } else if (createIfNotExists) {
            //Create the tenant if not exists
            this.create(kmsDomain);
            return true;
        }

        return false;
    }

    @Override
    public KmsDomain updateAdminStatus(String tenant, IEnumEnabledBinaryStatus.Types newStatus) {
        repository().updateAdminStatus(tenant, newStatus);
        return repository().findByNameIgnoreCase(tenant).orElse(null);
    }

    @Override
    public boolean isEnabled(String tenant) {
        return repository().getAdminStatus(tenant) == IEnumEnabledBinaryStatus.Types.ENABLED;
    }
}

package eu.isygoit.service.impl;

import eu.isygoit.annotation.CodeGenLocal;
import eu.isygoit.annotation.SrvRepo;
import eu.isygoit.com.rest.service.impl.CrudService;
import eu.isygoit.enums.IEnumBinaryStatus;
import eu.isygoit.model.Account;
import eu.isygoit.model.KmsDomain;
import eu.isygoit.repository.AccountRepository;
import eu.isygoit.repository.DomainRepository;
import eu.isygoit.service.IDomainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * The type Domain service.
 */
@Service
@Transactional
@CodeGenLocal(value = NextCodeService.class)
@SrvRepo(value = DomainRepository.class)
public class DomainService extends CrudService<Long, KmsDomain, DomainRepository> implements IDomainService {

    private final AccountService accountService;

    @Autowired
    public DomainService(AccountService accountService) {
        this.accountService = accountService;
    }

    @Override
    public Optional<KmsDomain> checkIfExists(String domainName, String domainUrl, boolean createIfNotExists) {
        return repository().findByNameIgnoreCase(domainName).map(domain -> {
            domain.setUrl(domainUrl);
            update(domain);
            return Optional.ofNullable(domain);
        }).orElseGet(() -> {
            if (createIfNotExists) {
                var newDomain = KmsDomain.builder()
                        .name(domainName)
                        .url(domainUrl)
                        .description(domainName)
                        .build();
                return Optional.ofNullable(create(newDomain));
            }
            return Optional.empty();
        });
    }

    @Override
    public Optional<KmsDomain> findByNameIgnoreCase(String domainName) {
        return repository().findByNameIgnoreCase(domainName);
    }

    @Override
    public Optional<Account> checkAccountIfExists(String domainName, String domainUrl, String email, String userName, String fullName, boolean createIfNotExists) {
        return checkIfExists(domainName, domainUrl, createIfNotExists)
                .flatMap(kmsDomain -> accountService.checkIfExists(
                        Account.builder()
                                .domain(domainName)
                                .fullName(fullName)
                                .email(email)
                                .code(userName)
                                .build(),
                        createIfNotExists));
    }

    @Override
    public Optional<KmsDomain> checkIfExists(KmsDomain kmsDomain, boolean createIfNotExists) {
        return repository().findByNameIgnoreCase(kmsDomain.getName()).map(domain -> {
            domain.setUrl(kmsDomain.getUrl());
            return Optional.ofNullable(update(domain));
        }).orElseGet(() -> {
            if (createIfNotExists) {
                return Optional.ofNullable(create(kmsDomain));
            }
            return Optional.empty();
        });
    }

    @Override
    public Optional<KmsDomain> updateAdminStatus(String domain, IEnumBinaryStatus.Types newStatus) {
        repository().updateAdminStatus(domain, newStatus);
        return repository().findByNameIgnoreCase(domain);
    }

    @Override
    public boolean isEnabled(String domain) {
        return repository().getAdminStatus(domain) == IEnumBinaryStatus.Types.ENABLED;
    }
}
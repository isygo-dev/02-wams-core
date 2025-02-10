package eu.isygoit.service.impl;


import eu.isygoit.annotation.SrvRepo;
import eu.isygoit.app.ApplicationContextService;
import eu.isygoit.com.rest.service.impl.CrudServiceUtils;
import eu.isygoit.exception.AccountNotFoundException;
import eu.isygoit.exception.PropertyNotFoundException;
import eu.isygoit.model.Account;
import eu.isygoit.model.AccountProps;
import eu.isygoit.model.Property;
import eu.isygoit.repository.AccountRepository;
import eu.isygoit.repository.AcountPropsRepository;
import eu.isygoit.repository.PropertyRepository;
import eu.isygoit.service.IPropertyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;


/**
 * The type Property service.
 */
@Slf4j
@Service
@Transactional
@SrvRepo(value = PropertyRepository.class)
public class PropertyService extends CrudServiceUtils<Property, PropertyRepository> implements IPropertyService {

    private final ApplicationContextService applicationContextService;
    @Override
    protected ApplicationContextService getApplicationContextServiceInstance() {
        return applicationContextService;
    }

    private final AccountRepository accountRepository;

    private final AcountPropsRepository acountPropsRepository;

    @Autowired
    public PropertyService(ApplicationContextService applicationContextService, AccountRepository accountRepository, AcountPropsRepository acountPropsRepository) {
        this.applicationContextService = applicationContextService;
        this.accountRepository = accountRepository;
        this.acountPropsRepository = acountPropsRepository;
    }


    @Override
    public Property updateProperty(String accountCode, Property property) {
        Optional<Account> optionalAccount = accountRepository.findByCodeIgnoreCase(accountCode);
        if (optionalAccount.isPresent()) {
            Account account = optionalAccount.get();
            Optional<Property> optional = repository().findByAccountAndGuiNameAndName(accountCode, property.getGuiName(), property.getName());
            if (optional.isPresent()) {
                // var .get
                optional.get().setValue(property.getValue());
                return repository().save(optional.get());
            } else {
                Optional<AccountProps> accountPropsOptional = acountPropsRepository.findByAccount_CodeIgnoreCase(accountCode);
                if (accountPropsOptional.isPresent()) {
                    AccountProps accountProps = accountPropsOptional.get();
                    if (CollectionUtils.isEmpty(accountProps.getProperties())) {
                        accountProps.setProperties(new ArrayList<>());
                    }
                    accountProps.getProperties().add(property);
                    acountPropsRepository.save(accountProps);
                } else {
                    AccountProps accountProps = AccountProps.builder()
                            .account(account)
                            .properties(Arrays.asList(property))
                            .build();
                    acountPropsRepository.save(accountProps);
                }

                return property;
            }
        } else {
            throw new AccountNotFoundException("with code: " + accountCode);
        }
    }

    @Override
    public Property getPropertyByAccount(String accountCode, String guiName, String name) {
        Optional<Account> optionalAccount = accountRepository.findByCodeIgnoreCase(accountCode);
        if (optionalAccount.isPresent()) {
            Optional<Property> optional = repository().findByAccountAndGuiNameAndName(accountCode, guiName, name);
            if (optional.isPresent()) {
                return optional.get();
            }
            throw new PropertyNotFoundException("with name: " + accountCode + "/" + guiName + "/" + name);
        } else {
            throw new AccountNotFoundException("with code: " + accountCode);
        }
    }

    @Override
    public List<Property> getPropertyByAccountAndGui(String accountCode, String guiName) {
        Optional<Account> optionalAccount = accountRepository.findByCodeIgnoreCase(accountCode);
        if (optionalAccount.isPresent()) {
            List<Property> listProperty = repository().findByAccountAndGuiName(accountCode, guiName);
            if (!CollectionUtils.isEmpty(listProperty)) {
                return listProperty;
            }
            throw new PropertyNotFoundException("with name: " + accountCode + "/" + guiName);
        } else {
            throw new AccountNotFoundException("with code: " + accountCode);
        }
    }
}

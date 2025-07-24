package eu.isygoit.service.impl;

import eu.isygoit.annotation.InjectCodeGen;
import eu.isygoit.annotation.InjectCodeGenKms;
import eu.isygoit.annotation.InjectRepository;
import eu.isygoit.com.rest.service.tenancy.ImageTenantService;
import eu.isygoit.config.AppProperties;
import eu.isygoit.constants.TenantConstants;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.exception.AccountNotFoundException;
import eu.isygoit.exception.CustomerNotFoundException;
import eu.isygoit.model.AppNextCode;
import eu.isygoit.model.Customer;
import eu.isygoit.model.schema.SchemaColumnConstantName;
import eu.isygoit.remote.kms.KmsIncrementalKeyService;
import eu.isygoit.repository.AccountRepository;
import eu.isygoit.repository.CustomerRepository;
import eu.isygoit.service.ICustomerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * The type Customer service.
 */
@Slf4j
@Service
@Transactional
@InjectCodeGen(value = NextCodeService.class)
@InjectCodeGenKms(value = KmsIncrementalKeyService.class)
@InjectRepository(value = CustomerRepository.class)
public class CustomerService extends ImageTenantService<Long, Customer, CustomerRepository> implements ICustomerService {

    private final AppProperties appProperties;
    private final AccountRepository accountRepository;


    /**
     * Instantiates a new Customer service.
     *
     * @param appProperties     the app properties
     * @param accountRepository the account repository
     */
    public CustomerService(AppProperties appProperties, AccountRepository accountRepository) {
        this.appProperties = appProperties;
        this.accountRepository = accountRepository;
    }

    @Override
    public AppNextCode initCodeGenerator() {
        return AppNextCode.builder()
                .tenant(TenantConstants.DEFAULT_TENANT_NAME)
                .entity(Customer.class.getSimpleName())
                .attribute(SchemaColumnConstantName.C_CODE)
                .prefix("CUS")
                .valueLength(6L)
                .codeValue(1L)
                .build();
    }

    @Override
    public List<String> getNames() {
        return repository().findAll().stream().map(customer -> customer.getName()).toList();
    }

    @Override
    public Customer updateStatus(Long id, IEnumEnabledBinaryStatus.Types newStatus) {
        repository().updateAdminStatusById(id, newStatus);
        return repository().findById(id).orElse(null);
    }

    @Override
    public Customer linkToAccount(String tenant, Long id, String accountCode) {
        Optional<Customer> optional = this.findById(tenant, id);
        if (optional.isPresent()) {
            Customer customer = optional.get();
            if (!accountRepository.existsByCodeIgnoreCase(accountCode)) {
                throw new AccountNotFoundException("with code:" + accountCode);
            }

            customer.setAccountCode(accountCode);
            return this.update(tenant, customer);
        } else {
            throw new CustomerNotFoundException("with id:" + id);
        }
    }

    @Override
    protected String getUploadDirectory() {
        return this.appProperties.getUploadDirectory();
    }
}

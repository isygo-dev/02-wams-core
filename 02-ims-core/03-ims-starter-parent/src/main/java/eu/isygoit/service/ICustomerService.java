package eu.isygoit.service;

import eu.isygoit.com.rest.controller.impl.tenancy.IImageTenantServiceMethods;
import eu.isygoit.com.rest.service.tenancy.ICrudTenantServiceMethods;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.model.Customer;

import java.util.List;

/**
 * The interface Customer service.
 */
public interface ICustomerService extends ICrudTenantServiceMethods<Long, Customer>,
        IImageTenantServiceMethods<Long, Customer> {

    /**
     * Gets names.
     *
     * @return the names
     */
    List<String> getNames();

    /**
     * Update status customer.
     *
     * @param id        the id
     * @param newStatus the new status
     * @return the customer
     */
    Customer updateStatus(Long id, IEnumEnabledBinaryStatus.Types newStatus);

    /**
     * Link to account customer.
     *
     * @param id          the id
     * @param accountCode the account code
     * @return the customer
     */
    Customer linkToAccount(String tenant, Long id, String accountCode);
}

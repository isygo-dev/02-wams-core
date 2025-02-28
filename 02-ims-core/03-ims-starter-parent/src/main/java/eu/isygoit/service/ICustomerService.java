package eu.isygoit.service;

import eu.isygoit.com.rest.service.ICrudServiceMethod;
import eu.isygoit.com.rest.service.IImageServiceMethods;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.model.Customer;

import java.util.List;

/**
 * The interface Customer service.
 */
public interface ICustomerService extends ICrudServiceMethod<Long, Customer>, IImageServiceMethods<Long, Customer> {

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
    Customer linkToAccount(Long id, String accountCode);
}

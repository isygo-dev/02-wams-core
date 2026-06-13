package eu.isygoit.remote.ims;

import eu.isygoit.com.rest.api.IMappedCrudApi;
import eu.isygoit.config.FeignConfig;
import eu.isygoit.dto.data.CustomerDto;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(configuration = FeignConfig.class, name = "identity-service", contextId = "customer", path = "/api/v1/private/customer")
public interface CustomerService extends IMappedCrudApi<Long, CustomerDto, CustomerDto> {

    @PutMapping("/update-status")
    CustomerDto updateCustomerStatus(@RequestParam("id") Long id,
                                     @RequestParam("newStatus") IEnumEnabledBinaryStatus.Types newStatus);

    @PutMapping("/link-account")
    CustomerDto linkToExistingAccount(@RequestParam("id") Long id,
                                      @RequestParam("accountCode") String accountCode);

    @GetMapping("/names")
    List<String> getCustomersNames();
}
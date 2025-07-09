package eu.isygoit.controller;

import eu.isygoit.annotation.InjectMapperAndService;
import eu.isygoit.com.rest.controller.impl.MappedImageController;
import eu.isygoit.com.rest.controller.impl.tenancy.MappedImageTenantController;
import eu.isygoit.dto.data.CustomerDto;
import eu.isygoit.exception.handler.ImsExceptionHandler;
import eu.isygoit.mapper.CustomerMapper;
import eu.isygoit.model.Customer;
import eu.isygoit.service.impl.CustomerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The type Customer image controller.
 */
@Slf4j
@Validated
@RestController
@InjectMapperAndService(handler = ImsExceptionHandler.class, mapper = CustomerMapper.class, minMapper = CustomerMapper.class, service = CustomerService.class)
@RequestMapping(path = "/api/v1/private/customer")
public class CustomerImageController extends MappedImageTenantController<Long, Customer, CustomerDto, CustomerDto, CustomerService> {

}

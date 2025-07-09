package eu.isygoit.controller;

import eu.isygoit.annotation.InjectMapperAndService;
import eu.isygoit.com.rest.controller.impl.MappedCrudController;
import eu.isygoit.dto.data.AccountDetailsDto;
import eu.isygoit.exception.handler.ImsExceptionHandler;
import eu.isygoit.mapper.AccountDetailsMapper;
import eu.isygoit.model.AccountDetails;
import eu.isygoit.service.impl.AccountDetailsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The type Account details controller.
 */
@Slf4j
@Validated
@RestController
@InjectMapperAndService(handler = ImsExceptionHandler.class, mapper = AccountDetailsMapper.class, minMapper = AccountDetailsMapper.class, service = AccountDetailsService.class)
@RequestMapping(path = "/api/v1/private/account/details")
public class AccountDetailsController extends MappedCrudController<Long, AccountDetails, AccountDetailsDto, AccountDetailsDto, AccountDetailsService> {
}

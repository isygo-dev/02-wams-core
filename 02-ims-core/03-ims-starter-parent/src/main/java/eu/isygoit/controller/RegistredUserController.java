package eu.isygoit.controller;

import eu.isygoit.annotation.InjectMapperAndService;
import eu.isygoit.api.RegisteredUserServiceApi;
import eu.isygoit.com.rest.controller.impl.tenancy.MappedCrudTenantController;
import eu.isygoit.dto.request.RegisteredUserDto;
import eu.isygoit.exception.handler.ImsExceptionHandler;
import eu.isygoit.mapper.RegistredUserMapper;
import eu.isygoit.model.RegisteredUser;
import eu.isygoit.service.impl.RegisteredUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The type Account controller.
 */
@Slf4j
@Validated
@RestController
@InjectMapperAndService(handler = ImsExceptionHandler.class, mapper = RegistredUserMapper.class, minMapper = RegistredUserMapper.class, service = RegisteredUserService.class)
@RequestMapping(path = "/api/v1/private/registred-user")
public class RegistredUserController extends MappedCrudTenantController<Long, RegisteredUser, RegisteredUserDto, RegisteredUserDto, RegisteredUserService>
        implements RegisteredUserServiceApi {

}

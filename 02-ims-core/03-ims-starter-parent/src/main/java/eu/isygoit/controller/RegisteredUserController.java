package eu.isygoit.controller;

import eu.isygoit.annotation.InjectMapperAndService;
import eu.isygoit.api.RegisteredUserServiceApi;
import eu.isygoit.com.rest.controller.impl.tenancy.MappedCrudTenantController;
import eu.isygoit.dto.data.AccountDto;
import eu.isygoit.dto.request.CreateAccountFromRegisteredRequestDto;
import eu.isygoit.dto.request.RegisteredUserDto;
import eu.isygoit.exception.handler.ImsExceptionHandler;
import eu.isygoit.mapper.AccountMapper;
import eu.isygoit.mapper.RegisteredUserMapper;
import eu.isygoit.model.RegisteredUser;
import eu.isygoit.service.impl.RegisteredUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The type Account controller.
 */
@Slf4j
@Validated
@RestController
@InjectMapperAndService(handler = ImsExceptionHandler.class, mapper = RegisteredUserMapper.class, minMapper = RegisteredUserMapper.class, service = RegisteredUserService.class)
@RequestMapping(path = "/api/v1/private/registred-user")
public class RegisteredUserController extends MappedCrudTenantController<Long, RegisteredUser, RegisteredUserDto, RegisteredUserDto, RegisteredUserService>
        implements RegisteredUserServiceApi {

    @Autowired
    private AccountMapper accountMapper;

    @Override
    public ResponseEntity<AccountDto> createAccountFromRegistered(
            @RequestBody CreateAccountFromRegisteredRequestDto request) {
        log.info("Creating account from registered user with email: {}", request.getEmail());

        try {
            return ResponseEntity.ok(accountMapper.entityToDto(crudService().createAccountFromRegistered(
                    requestContextService().getCurrentContext().getSenderTenant(),
                    request)));
        } catch (Throwable e) {
            log.error("<Error>: Call api createAccountFromRegistered {} / {}", request.getEmail(), e);
            return getBackExceptionResponse(e);
        }
    }
}

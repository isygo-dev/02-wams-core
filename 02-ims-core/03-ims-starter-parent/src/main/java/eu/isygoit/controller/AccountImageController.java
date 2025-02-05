package eu.isygoit.controller;

import eu.isygoit.annotation.CtrlDef;
import eu.isygoit.com.rest.controller.impl.MappedImageController;
import eu.isygoit.dto.data.AccountDto;
import eu.isygoit.dto.request.UpdateAccountRequestDto;
import eu.isygoit.exception.handler.ImsExceptionHandler;
import eu.isygoit.mapper.AccountMapper;
import eu.isygoit.model.Account;
import eu.isygoit.remote.kms.KmsPasswordService;
import eu.isygoit.service.impl.AccountService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The type Account image controller.
 */
@Slf4j
@Validated
@RestController
@CtrlDef(handler = ImsExceptionHandler.class, mapper = AccountMapper.class, minMapper = AccountMapper.class, service = AccountService.class)
@RequestMapping(path = "/api/v1/private/account")
public class AccountImageController extends MappedImageController<Long, Account, AccountDto, AccountDto, AccountService> {

    @Autowired
    private KmsPasswordService kmsPasswordService;

    @Override
    public AccountDto beforeUpdate(AccountDto account) throws Exception {
        try {
            ResponseEntity<UpdateAccountRequestDto> result = kmsPasswordService.updateAccount(//RequestContextDto.builder().build(),
                    UpdateAccountRequestDto.builder()
                            .code(account.getCode())
                            .domain(account.getDomain())
                            .email(account.getEmail())
                            .fullName(account.getFullName())
                            .build());
            if (result.getStatusCode().is2xxSuccessful() && result.hasBody()) {
                return super.beforeUpdate(account);
            }
        } catch (Exception e) {
            log.error("Remote feign call failed : ", e);
            //throw new RemoteCallFailedException(e);
        }

        return super.beforeUpdate(account);
    }
}

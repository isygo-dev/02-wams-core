package eu.isygoit.controller;

import eu.isygoit.annotation.InjectMapperAndService;
import eu.isygoit.api.AccountServiceApi;
import eu.isygoit.api.RegisteredUserServiceApi;
import eu.isygoit.api.StatisticServiceApi;
import eu.isygoit.com.rest.controller.ResponseFactory;
import eu.isygoit.com.rest.controller.constants.CtrlConstants;
import eu.isygoit.com.rest.controller.impl.tenancy.MappedCrudTenantController;
import eu.isygoit.dto.common.ResetPwdViaTokenRequestDto;
import eu.isygoit.dto.data.AccountDto;
import eu.isygoit.dto.data.MinAccountDto;
import eu.isygoit.dto.data.TenantAdminDto;
import eu.isygoit.dto.request.GeneratePwdRequestDto;
import eu.isygoit.dto.request.RegisteredUserDto;
import eu.isygoit.dto.request.UpdateAccountRequestDto;
import eu.isygoit.dto.response.UserDataResponseDto;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.enums.IEnumLanguage;
import eu.isygoit.exception.handler.ImsExceptionHandler;
import eu.isygoit.mapper.AccountMapper;
import eu.isygoit.mapper.MinAccountMapper;
import eu.isygoit.mapper.RegistredUserMapper;
import eu.isygoit.model.Account;
import eu.isygoit.model.RegisteredUser;
import eu.isygoit.model.Tenant;
import eu.isygoit.remote.kms.KmsPasswordService;
import eu.isygoit.service.IAccountService;
import eu.isygoit.service.ITenantService;
import eu.isygoit.service.RequestContextService;
import eu.isygoit.service.impl.AccountService;
import eu.isygoit.service.impl.RegisteredUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

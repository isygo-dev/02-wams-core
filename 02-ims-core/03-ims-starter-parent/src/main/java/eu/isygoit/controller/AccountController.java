package eu.isygoit.controller;

import eu.isygoit.annotation.InjectMapperAndService;
import eu.isygoit.api.AccountControllerApi;
import eu.isygoit.api.StatisticControllerApi;
import eu.isygoit.com.rest.controller.ResponseFactory;
import eu.isygoit.com.rest.controller.constants.CtrlConstants;
import eu.isygoit.com.rest.controller.impl.MappedCrudController;
import eu.isygoit.com.rest.controller.impl.tenancy.MappedCrudTenantController;
import eu.isygoit.dto.common.RequestContextDto;
import eu.isygoit.dto.common.ResetPwdViaTokenRequestDto;
import eu.isygoit.dto.data.AccountDto;
import eu.isygoit.dto.data.DomainAdminDto;
import eu.isygoit.dto.data.MinAccountDto;
import eu.isygoit.dto.request.GeneratePwdRequestDto;
import eu.isygoit.dto.request.UpdateAccountRequestDto;
import eu.isygoit.dto.response.UserDataResponseDto;
import eu.isygoit.enums.IEnumAuth;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.enums.IEnumLanguage;
import eu.isygoit.exception.handler.ImsExceptionHandler;
import eu.isygoit.jwt.JwtService;
import eu.isygoit.mapper.AccountMapper;
import eu.isygoit.mapper.MinAccountMapper;
import eu.isygoit.mapper.ThemeMapper;
import eu.isygoit.model.Account;
import eu.isygoit.model.Domain;
import eu.isygoit.remote.kms.KmsPasswordService;
import eu.isygoit.service.IAccountService;
import eu.isygoit.service.IDomainService;
import eu.isygoit.service.IThemeService;
import eu.isygoit.service.impl.AccountService;
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
@InjectMapperAndService(handler = ImsExceptionHandler.class, mapper = AccountMapper.class, minMapper = MinAccountMapper.class, service = AccountService.class)
@RequestMapping(path = "/api/v1/private/account")
public class AccountController extends MappedCrudTenantController<Long, Account, MinAccountDto, AccountDto, AccountService>
        implements AccountControllerApi, StatisticControllerApi {

    @Autowired
    private KmsPasswordService kmsPasswordService;
    @Autowired
    private IAccountService accountService;
    @Autowired
    private IDomainService tenantService;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private MinAccountMapper minAccountMapper;
    @Autowired
    private IThemeService themeService;
    @Autowired
    private ThemeMapper themeMapper;

    @Override
    public AccountDto beforeUpdate(Long id, AccountDto account) {
        try {
            ResponseEntity<Boolean> result = kmsPasswordService.updateAccount(//RequestContextDto.builder().build(),
                    UpdateAccountRequestDto.builder()
                            .code(account.getCode())
                            .tenant(account.getTenant())
                            .email(account.getEmail())
                            .fullName(account.getFullName())
                            .build());
            if (result.getStatusCode().is2xxSuccessful() && result.hasBody() && result.getBody()) {
                return super.beforeUpdate(id, account);
            }
        } catch (Exception e) {
            log.error("Remote feign call failed : ", e);
            //throw new RemoteCallFailedException(e);
        }
        return super.beforeUpdate(id, account);
    }

    @Override
    public Account afterCreate(Account account) {
        try {
            ResponseEntity<Integer> result = kmsPasswordService.generate(//RequestContextDto.builder().build(),
                    IEnumAuth.Types.PWD,
                    GeneratePwdRequestDto.builder()
                            .tenant(account.getTenant())
                            .tenantUrl(tenantService.findByName(account.getTenant()).getUrl())
                            .email(account.getEmail())
                            .userName(account.getCode())
                            .fullName(account.getFullName())
                            .build());
            if (result.getStatusCode().is2xxSuccessful() && result.hasBody()) {
                return super.afterCreate(account);
            }
        } catch (Exception e) {
            log.error("Remote feign call failed : ", e);
            //throw new RemoteCallFailedException(e);
        }
        return super.afterCreate(account);
    }

    @Override
    public ResponseEntity<List<String>> getEmailsByTenant(RequestContextDto requestContext) {
        log.info("get accounts email by tenant");
        try {
            return ResponseFactory.responseOk(accountService.findEmailsByTenant(requestContext.getSenderTenant()));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    //TODO use new Controller struct wit MinDto/FullDto
    public ResponseEntity<List<MinAccountDto>> getAccounts(RequestContextDto requestContext) {
        log.info("get accounts mini data");
        try {
            return ResponseFactory.responseOk(accountService.getMinInfoByTenant(requestContext.getSenderTenant()));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<AccountDto> updateAccountAdminStatus(RequestContextDto requestContext,
                                                               Long id,
                                                               IEnumEnabledBinaryStatus.Types newStatus) {
        log.info("update account admin status");
        try {
            return ResponseFactory.responseOk(mapper().entityToDto(accountService.updateAccountAdminStatus(id, newStatus)));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<AccountDto> createDomainAdmin(RequestContextDto requestContext, String tenant, DomainAdminDto admin) {
        log.info("create tenant admin");
        try {
            return ResponseFactory.responseOk(mapper().entityToDto(accountService.createDomainAdmin(tenant, admin)));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<AccountDto> updateAccountIsAdmin(RequestContextDto requestContext,
                                                           Long id,
                                                           boolean newStatus) {
        log.info("update account isAdmin");
        try {
            return ResponseFactory.responseOk(mapper().entityToDto(accountService.updateAccountIsAdmin(id, newStatus)));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<AccountDto> updateLanguage(RequestContextDto requestContext,
                                                     Long id,
                                                     IEnumLanguage.Types language) {
        log.info("update account language");
        try {
            return ResponseFactory.responseOk(mapper().entityToDto(accountService.updateLanguage(id, language)));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    //TODO Refactor this API to return AuthResponseDto/SystemInfo
    @Override
    public ResponseEntity<UserDataResponseDto> connectedUser(RequestContextDto requestContext) {
        try {
            Account account = accountService.findByTenantAndUserName(requestContext.getSenderTenant(), requestContext.getSenderUser());
            Domain tenant = tenantService.findByName(requestContext.getSenderTenant());
            //ThemeDto theme = themeMapper.entityToDto(themeService.findThemeByAccountCodeAndDomainCode(account.getCode(), tenant.getCode()));
            UserDataResponseDto userDataResponseDto = UserDataResponseDto.builder()
                    .id(account.getId())
                    .userName(account.getCode())
                    .firstName(account.getAccountDetails().getFirstName())
                    .lastName(account.getAccountDetails().getLastName())
                    //.applications(accountService.buildAllowedTools(account, authenticate.getAccessToken()))
                    .email(account.getEmail())
                    .tenantId(tenant.getId())
                    .tenantImagePath(tenant.getImagePath())
                    .language(account.getLanguage())
                    .role(account.getFunctionRole())
                    .build();

            /*
            return ResponseFactory.responseOk(AuthResponseDto.builder()
                    .tokenType(IEnumWebToken.Types.Bearer)
                    .accessToken(authenticate.getAccessToken())
                    .refreshToken(authenticate.getRefreshToken())
                    .userDataResponseDto(userDataResponseDto)
                    .theme(theme)
                    .systemInfo(SystemInfoDto
                            .builder()
                            .name(appProperties.getApplicationName())
                            .version(appProperties.getApplicationVersion())
                            .build())
                    .build());
             */

            return ResponseFactory.responseOk(userDataResponseDto);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<AccountDto> connectedUserFullData(RequestContextDto requestContext) {
        try {
            return ResponseFactory.responseOk(mapper().entityToDto(accountService.findByTenantAndUserName(requestContext.getSenderTenant(),
                    requestContext.getSenderUser())));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<AccountDto> updateConnectedUserAccountData(RequestContextDto requestContext,
                                                                     AccountDto accountDto) {
        try {
            Account account = accountService.findByTenantAndUserName(requestContext.getSenderTenant(), requestContext.getSenderUser());
            accountDto.setId(account.getId());
            this.beforeUpdate(accountDto.getId(), accountDto);
            return ResponseFactory.responseOk(mapper().entityToDto(accountService.update(requestContext.getSenderTenant(),
                    mapper().dtoToEntity(accountDto))));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<AccountDto> updateUserAccountData(RequestContextDto requestContext,
                                                            Long id,
                                                            AccountDto accountDto) {
        try {
            accountDto.setId(id);
            this.beforeUpdate(accountDto.getId(), accountDto);
            return ResponseFactory.responseOk(mapper().entityToDto(accountService.update(requestContext.getSenderTenant(),mapper().dtoToEntity(accountDto))));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<String> resetPasswordViaToken(RequestContextDto requestContext,
                                                        ResetPwdViaTokenRequestDto resetPasswordViaTokenRequest) {
        try {
            return kmsPasswordService.resetPasswordViaToken(//RequestContextDto.builder().build(),
                    resetPasswordViaTokenRequest);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<List<MinAccountDto>> accountsByTenant(RequestContextDto requestContext) {
        log.info("get accounts by sender tenant");
        try {
            List<MinAccountDto> list = minAccountMapper.listEntityToDto(accountService.getByTenant(requestContext.getSenderTenant()));
            if (CollectionUtils.isEmpty(list)) {
                return ResponseFactory.responseNoContent();
            }
            return ResponseFactory.responseOk(list);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<List<MinAccountDto>> userAccountsByTenant(RequestContextDto requestContext, String tenant) {
        log.info("get accounts by tenant");
        try {
            List<MinAccountDto> list = minAccountMapper.listEntityToDto(accountService.getByTenant(tenant));
            if (CollectionUtils.isEmpty(list)) {
                return ResponseFactory.responseNoContent();
            }
            return ResponseFactory.responseOk(list);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<List<MinAccountDto>> chatAccountsByTenant(RequestContextDto requestContext) {
        log.info("get chat accounts by tenant");
        try {
            List<MinAccountDto> list = minAccountMapper.listEntityToDto(accountService.chatAccountsByTenant(requestContext.getSenderTenant()));
            if (CollectionUtils.isEmpty(list)) {
                return ResponseFactory.responseNoContent();
            }
            return ResponseFactory.responseOk(list);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<?> resendCreationEmail(RequestContextDto requestContext, Long id) {
        try {
            accountService.resendCreationEmail(requestContext.getSenderTenant(), id);
            return ResponseFactory.responseOk();
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<Long> getConfirmedResumeAccountsCount(RequestContextDto requestContext) {
        try {
            return ResponseFactory.responseOk(accountService.stat_GetConfirmedResumeAccountsCount(requestContext));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<Long> getConfirmedAccountNumberByEmployee(RequestContextDto requestContext) {
        try {
            return ResponseFactory.responseOk(accountService.stat_GetConfirmedEmployeeAccountsCount(requestContext));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }
}

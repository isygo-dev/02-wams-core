package eu.isygoit.controller;

import eu.isygoit.annotation.CtrlDef;
import eu.isygoit.api.AccountControllerApi;
import eu.isygoit.api.StatisticControllerApi;
import eu.isygoit.app.ApplicationContextService;
import eu.isygoit.com.rest.controller.ResponseFactory;
import eu.isygoit.com.rest.controller.constants.CtrlConstants;
import eu.isygoit.com.rest.controller.impl.MappedCrudController;
import eu.isygoit.dto.common.RequestContextDto;
import eu.isygoit.dto.common.ResetPwdViaTokenRequestDto;
import eu.isygoit.dto.data.AccountDto;
import eu.isygoit.dto.data.DomainAdminDto;
import eu.isygoit.dto.data.MinAccountDto;
import eu.isygoit.dto.request.GeneratePwdRequestDto;
import eu.isygoit.dto.request.UpdateAccountRequestDto;
import eu.isygoit.dto.response.UserDataResponseDto;
import eu.isygoit.enums.IEnumAuth;
import eu.isygoit.enums.IEnumBinaryStatus;
import eu.isygoit.enums.IEnumLanguage;
import eu.isygoit.exception.AccountNotFoundException;
import eu.isygoit.exception.DomainNotFoundException;
import eu.isygoit.exception.handler.ImsExceptionHandler;
import eu.isygoit.mapper.AccountMapper;
import eu.isygoit.mapper.MinAccountMapper;
import eu.isygoit.model.Account;
import eu.isygoit.model.Domain;
import eu.isygoit.remote.kms.KmsPasswordService;
import eu.isygoit.service.IAccountService;
import eu.isygoit.service.IDomainService;
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
@CtrlDef(handler = ImsExceptionHandler.class, mapper = AccountMapper.class, minMapper = MinAccountMapper.class, service = AccountService.class)
@RequestMapping(path = "/api/v1/private/account")
public class AccountController extends MappedCrudController<Long, Account, MinAccountDto, AccountDto, AccountService>
        implements AccountControllerApi, StatisticControllerApi {

    private final ApplicationContextService applicationContextService;
    private final KmsPasswordService kmsPasswordService;
    private final IAccountService accountService;
    private final IDomainService domainService;
    private final MinAccountMapper minAccountMapper;
    @Autowired
    public AccountController(ApplicationContextService applicationContextService, KmsPasswordService kmsPasswordService, IAccountService accountService, IDomainService domainService, MinAccountMapper minAccountMapper) {
        this.applicationContextService = applicationContextService;
        this.kmsPasswordService = kmsPasswordService;
        this.accountService = accountService;
        this.domainService = domainService;
        this.minAccountMapper = minAccountMapper;
    }

    @Override
    protected ApplicationContextService getApplicationContextServiceInstance() {
        return applicationContextService;
    }

    @Override
    public AccountDto beforeUpdate(Long id, AccountDto account) {
        try {
            ResponseEntity<UpdateAccountRequestDto> result = kmsPasswordService.updateAccount(//RequestContextDto.builder().build(),
                    UpdateAccountRequestDto.builder()
                            .code(account.getCode())
                            .domain(account.getDomain())
                            .email(account.getEmail())
                            .fullName(account.getFullName())
                            .build());
            if (result.getStatusCode().is2xxSuccessful() && result.hasBody()) {
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
                            .domain(account.getDomain())
                            .domainUrl(domainService.getByName(account.getDomain())
                                    .orElseThrow(() -> new DomainNotFoundException("with name " + account.getDomain())).getUrl())
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
    public ResponseEntity<List<String>> getEmailsByDomain(RequestContextDto requestContext) {
        log.info("get accounts email by domain");
        try {
            return ResponseFactory.ResponseOk(accountService.findEmailsByDomain(requestContext.getSenderDomain()));
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
            return ResponseFactory.ResponseOk(accountService.getMinInfoByDomain(requestContext.getSenderDomain()));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<AccountDto> updateAccountAdminStatus(RequestContextDto requestContext,
                                                               Long id,
                                                               IEnumBinaryStatus.Types newStatus) {
        log.info("update account admin status");
        try {
            return ResponseFactory.ResponseOk(mapper().entityToDto(accountService.updateAdminStatus(id, newStatus)));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<AccountDto> createDomainAdmin(RequestContextDto requestContext, String domain, DomainAdminDto admin) {
        log.info("create domain admin");
        try {
            return ResponseFactory.ResponseOk(mapper().entityToDto(accountService.createDomainAdminAccount(domain, admin)));
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
            return ResponseFactory.ResponseOk(mapper().entityToDto(accountService.updateIsAdmin(id, newStatus)));
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
            return ResponseFactory.ResponseOk(mapper().entityToDto(accountService.updateLanguage(id, language)));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    //TODO Refactor this API to return AuthResponseDto/SystemInfo
    @Override
    public ResponseEntity<UserDataResponseDto> connectedUser(RequestContextDto requestContext) {
        try {
            Account account = accountService.getByDomainAndUserName(requestContext.getSenderDomain(), requestContext.getSenderUser())
                    .orElseThrow(() -> new AccountNotFoundException("with domain " + requestContext.getSenderDomain()
                            + " and username " + requestContext.getSenderUser()));
            Domain domain = domainService.getByName(requestContext.getSenderDomain())
                    .orElseThrow(() -> new DomainNotFoundException("with domain " + requestContext.getSenderDomain()));
            //ThemeDto theme = themeMapper.entityToDto(themeService.findThemeByAccountCodeAndDomainCode(account.getCode(), domain.getCode()));
            UserDataResponseDto userDataResponseDto = UserDataResponseDto.builder()
                    .id(account.getId())
                    .userName(account.getCode())
                    .firstName(account.getAccountDetails().getFirstName())
                    .lastName(account.getAccountDetails().getLastName())
                    //.applications(accountService.buildAllowedTools(account, authenticate.getAccessToken()))
                    .email(account.getEmail())
                    .domainId(domain.getId())
                    .domainImagePath(domain.getImagePath())
                    .language(account.getLanguage())
                    .role(account.getFunctionRole())
                    .build();

            /*
            return ResponseFactory.ResponseOk(AuthResponseDto.builder()
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

            return ResponseFactory.ResponseOk(userDataResponseDto);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<AccountDto> connectedUserFullData(RequestContextDto requestContext) {
        try {
            return ResponseFactory.ResponseOk(mapper().entityToDto(accountService.getByDomainAndUserName(requestContext.getSenderDomain(),
                            requestContext.getSenderUser())
                    .orElseThrow(() -> new AccountNotFoundException("with domain " + requestContext.getSenderDomain()
                            + " and username " + requestContext.getSenderUser()))));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<AccountDto> updateConnectedUserAccountData(RequestContextDto requestContext,
                                                                     AccountDto accountDto) {
        try {
            accountDto.setId(accountService.getByDomainAndUserName(requestContext.getSenderDomain(), requestContext.getSenderUser())
                    .orElseThrow(() -> new AccountNotFoundException("with domain " + requestContext.getSenderDomain()
                            + " and username " + requestContext.getSenderUser())).getId());
            this.beforeUpdate(accountDto.getId(), accountDto);
            return ResponseFactory.ResponseOk(mapper().entityToDto(accountService.update(mapper().dtoToEntity(accountDto))));
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
            return ResponseFactory.ResponseOk(mapper().entityToDto(accountService.update(mapper().dtoToEntity(accountDto))));
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
    public ResponseEntity<List<MinAccountDto>> accountsByDomain(RequestContextDto requestContext) {
        log.info("get accounts by sender domain");
        try {
            List<MinAccountDto> list = minAccountMapper.listEntityToDto(accountService.getByDomain(requestContext.getSenderDomain()));
            if (CollectionUtils.isEmpty(list)) {
                return ResponseFactory.ResponseNoContent();
            }
            return ResponseFactory.ResponseOk(list);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<List<MinAccountDto>> userAccountsByDomain(RequestContextDto requestContext, String domain) {
        log.info("get accounts by domain");
        try {
            List<MinAccountDto> list = minAccountMapper.listEntityToDto(accountService.getByDomain(domain));
            if (CollectionUtils.isEmpty(list)) {
                return ResponseFactory.ResponseNoContent();
            }
            return ResponseFactory.ResponseOk(list);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<List<MinAccountDto>> chatAccountsByDomain(RequestContextDto requestContext) {
        log.info("get chat accounts by domain");
        try {
            List<MinAccountDto> list = minAccountMapper.listEntityToDto(accountService.getChatAccountsByDomain(requestContext.getSenderDomain()));
            if (CollectionUtils.isEmpty(list)) {
                return ResponseFactory.ResponseNoContent();
            }
            return ResponseFactory.ResponseOk(list);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<?> resendCreationEmail(RequestContextDto requestContext, Long id) {
        try {
            accountService.resendCreationEmail(id);
            return ResponseFactory.ResponseOk();
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<Long> getConfirmedResumeAccountsCount(RequestContextDto requestContext) {
        try {
            return ResponseFactory.ResponseOk(accountService.stat_GetConfirmedResumeAccountsCount(requestContext));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<Long> getConfirmedAccountNumberByEmployee(RequestContextDto requestContext) {
        try {
            return ResponseFactory.ResponseOk(accountService.stat_GetConfirmedEmployeeAccountsCount(requestContext));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }
}

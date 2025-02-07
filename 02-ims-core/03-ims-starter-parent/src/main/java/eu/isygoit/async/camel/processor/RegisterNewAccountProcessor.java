package eu.isygoit.async.camel.processor;

import eu.isygoit.com.camel.processor.AbstractCamelProcessor;
import eu.isygoit.com.camel.processor.AbstractStringProcessor;
import eu.isygoit.dto.data.AssoAccountDto;
import eu.isygoit.dto.request.NewAccountDto;
import eu.isygoit.exception.RoleNotDefinedException;
import eu.isygoit.helper.JsonHelper;
import eu.isygoit.model.Account;
import eu.isygoit.model.AccountDetails;
import eu.isygoit.model.RoleInfo;
import eu.isygoit.service.IAccountService;
import eu.isygoit.service.IAppParameterService;
import eu.isygoit.service.IRoleInfoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Objects;

/**
 * The type Register new account processor.
 */
@Slf4j
@Component
@Qualifier("registerNewAccountProcessor")
public class RegisterNewAccountProcessor extends AbstractStringProcessor {

    private final IAccountService accountService;
    private final IAppParameterService parameterService;
    private final IRoleInfoService roleInfoService;

    @Autowired
    public RegisterNewAccountProcessor(IAccountService accountService, IAppParameterService parameterService, IRoleInfoService roleInfoService) {
        this.accountService = accountService;
        this.parameterService = parameterService;
        this.roleInfoService = roleInfoService;
    }

    @Override
    public void performProcessor(Exchange exchange, String RegisterNewAccountMsg) throws Exception {
        NewAccountDto newAccount = JsonHelper.fromJson(RegisterNewAccountMsg, NewAccountDto.class);

        exchange.getIn().setHeader("email", newAccount.getEmail());
        exchange.getIn().setHeader("firstName", newAccount.getFirstName());
        exchange.getIn().setHeader("lastName", newAccount.getLastName());

        RoleInfo roleInfo = null;
        String[] splitOrigin = newAccount.getOrigin().split("-");
        String roleName = parameterService.getValueByDomainAndName(newAccount.getDomain(), splitOrigin[0] + "_ROLE", true, "");
        if (StringUtils.hasText(roleName)) {
            roleInfo = roleInfoService.findByName(roleName)
                    .orElseThrow(() -> new RoleNotDefinedException("with name " + roleName));
        } else {
            log.error("<Error>: No role parametrized for account origin : {} ", newAccount.getOrigin());
        }

        Account account = accountService.create(Account.builder()
                .origin(newAccount.getOrigin())
                .domain(newAccount.getDomain())
                .email(newAccount.getEmail())
                .phoneNumber(newAccount.getPhoneNumber())
                .roleInfo(Objects.nonNull(roleInfo) ? Arrays.asList(roleInfo) : null)
                .functionRole(newAccount.getFunctionRole())
                .accountDetails(AccountDetails.builder()
                        .firstName(newAccount.getFirstName())
                        .lastName(newAccount.getLastName())
                        .build())
                .build());

        exchange.getIn().setBody(JsonHelper.toJson(AssoAccountDto.builder()
                .code(account.getCode())
                .origin(account.getOrigin())
                .build()));
        exchange.getIn().setHeader(AbstractCamelProcessor.ORIGIN, splitOrigin[0]);
        exchange.getIn().setHeader(AbstractCamelProcessor.RETURN_HEADER, true);
    }
}

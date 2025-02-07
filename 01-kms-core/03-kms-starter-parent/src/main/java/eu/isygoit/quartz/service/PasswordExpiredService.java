package eu.isygoit.quartz.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.isygoit.config.AppProperties;
import eu.isygoit.dto.data.MailMessageDto;
import eu.isygoit.enums.IEnumAuth;
import eu.isygoit.enums.IEnumMsgTemplateName;
import eu.isygoit.enums.IEnumPasswordStatus;
import eu.isygoit.model.PasswordInfo;
import eu.isygoit.repository.AccountRepository;
import eu.isygoit.repository.PasswordInfoRepository;
import eu.isygoit.service.IDomainService;
import eu.isygoit.service.IMsgService;
import eu.isygoit.types.EmailSubjects;
import eu.isygoit.types.MsgTemplateVariables;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The type Password expired service.
 */
@Data
@Slf4j
@Service
public class PasswordExpiredService extends AbstractJobService {

    private final AppProperties appProperties;

    private final PasswordInfoRepository passwordInfoRepository;
    private final AccountRepository accountRepository;
    private final IDomainService domainService;
    private final IMsgService msgService;

    @Autowired
    public PasswordExpiredService(AppProperties appProperties, PasswordInfoRepository passwordInfoRepository, AccountRepository accountRepository, IDomainService domainService, IMsgService msgService) {
        this.appProperties = appProperties;
        this.passwordInfoRepository = passwordInfoRepository;
        this.accountRepository = accountRepository;
        this.domainService = domainService;
        this.msgService = msgService;
    }

    @Override
    public void performJob(JobExecutionContext jobExecutionContext) {
        List<PasswordInfo> passwordInfoList = passwordInfoRepository.findByStatusAndAuthType(
                IEnumPasswordStatus.Types.VALID, IEnumAuth.Types.PWD);

        if (CollectionUtils.isEmpty(passwordInfoList)) return;

        passwordInfoList.stream()
                .filter(passwordInfo -> {
                    var remainingDays = passwordInfo.remainingDays();
                    return remainingDays > 0 && remainingDays <= appProperties.getPwdExpiredLessRemainigDays();
                })
                .map(passwordInfo -> accountRepository.findById(passwordInfo.getUserId())
                        .map(account -> Map.entry(account, passwordInfo)))
                .flatMap(Optional::stream) // Unwraps Optional<Account>
                .forEach(entry -> {
                    var account = entry.getKey();
                    var passwordInfo = entry.getValue();

                    try {
                        var mailMessageDto = MailMessageDto.builder()
                                .subject(EmailSubjects.PASSWORD_WILL_EXPIRE_EMAIL_SUBJECT)
                                .domain(account.getDomain())
                                .toAddr(account.getEmail())
                                .templateName(IEnumMsgTemplateName.Types.PASSWORD_EXPIRE_TEMPLATE)
                                .variables(MailMessageDto.getVariablesAsString(Map.of(
                                        MsgTemplateVariables.V_USER_NAME, account.getCode(),
                                        MsgTemplateVariables.V_FULLNAME, account.getFullName(),
                                        MsgTemplateVariables.V_DOMAIN_NAME, account.getDomain(),
                                        MsgTemplateVariables.V_PWD_EXP_REMAINING_DAYS, passwordInfo.remainingDays().toString()
                                )))
                                .build();

                        msgService.sendMessage(account.getDomain(), mailMessageDto, appProperties.isSendAsyncEmail());
                    } catch (JsonProcessingException e) {
                        log.error("<Error>: send password expire email : {} ", e);
                    }
                });
    }
}

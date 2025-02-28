package eu.isygoit.quartz.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.isygoit.config.AppProperties;
import eu.isygoit.dto.data.MailMessageDto;
import eu.isygoit.enums.IEnumAuth;
import eu.isygoit.enums.IEnumEmailTemplate;
import eu.isygoit.enums.IEnumPasswordStatus;
import eu.isygoit.model.Account;
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

    @Autowired
    private PasswordInfoRepository passwordInfoRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private IDomainService domainService;
    @Autowired
    private IMsgService msgService;

    @Override
    public void performJob(JobExecutionContext jobExecutionContext) {
        List<PasswordInfo> list = passwordInfoRepository.findByStatusAndAuthType(IEnumPasswordStatus.Types.VALID, IEnumAuth.Types.PWD);
        if (!CollectionUtils.isEmpty(list)) {
            list.forEach(passwordInfo -> {
                if (passwordInfo.remainingDays() <= appProperties.getPwdExpiredLessRemainigDays() && passwordInfo.remainingDays() > 0) {
                    Optional<Account> optional = accountRepository.findById(passwordInfo.getUserId());
                    if (optional.isPresent()) {
                        Account account = optional.get();
                        MailMessageDto mailMessageDto = null;
                        try {
                            mailMessageDto = MailMessageDto.builder()
                                    .subject(EmailSubjects.PASSWORD_WILL_EXPIRE_EMAIL_SUBJECT)
                                    .domain(account.getDomain())
                                    .toAddr(account.getEmail())
                                    .templateName(IEnumEmailTemplate.Types.PASSWORD_EXPIRE_TEMPLATE)
                                    .variables(MailMessageDto.getVariablesAsString(Map.of(
                                            //Common vars
                                            MsgTemplateVariables.V_USER_NAME, account.getCode(),
                                            MsgTemplateVariables.V_FULLNAME, account.getFullName(),
                                            MsgTemplateVariables.V_DOMAIN_NAME, account.getDomain(),
                                            //Specific vars
                                            MsgTemplateVariables.V_PWD_EXP_REMAINING_DAYS, passwordInfo.remainingDays().toString())))
                                    .build();
                        } catch (JsonProcessingException e) {
                            log.error("<Error>: send password expire email : {} ", e);
                        }
                        //Send the email message
                        msgService.sendMessage(account.getDomain(), mailMessageDto, appProperties.isSendAsyncEmail());
                    }
                }
            });
        }
    }
}

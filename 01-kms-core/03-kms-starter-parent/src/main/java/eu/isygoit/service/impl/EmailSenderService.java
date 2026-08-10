package eu.isygoit.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.isygoit.config.AppProperties;
import eu.isygoit.dto.data.MailMessageDto;
import eu.isygoit.enums.IEnumEmailTemplate;
import eu.isygoit.model.Account;
import eu.isygoit.remote.ims.ImsAppParameterService;
import eu.isygoit.service.IMsgService;
import eu.isygoit.types.EmailSubjects;
import eu.isygoit.types.MsgTemplateVariables;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class EmailSenderService {

    private final AppProperties appProperties;

    @Autowired
    private IMsgService msgService;

    @Autowired
    private ImsAppParameterService imsAppParameterService;

    public EmailSenderService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    void sendOTPEmail(String tenant, Account account, String otpCode, Integer lifetime) throws JsonProcessingException {
        MailMessageDto mailMessageDto = MailMessageDto.builder()
                .subject(EmailSubjects.OTP_CODE_ACCESS_EMAIL_SUBJECT)
                .senderTenant(tenant)
                .toAddr(account.getEmail())
                .fromAddr("noreply@" + tenant + ".com")
                .templateName(IEnumEmailTemplate.Types.AUTH_OTP_TEMPLATE)
                .variables(MailMessageDto.getVariablesAsString(Map.of(
                        //Common vars
                        MsgTemplateVariables.V_USER_NAME, account.getCode(),
                        MsgTemplateVariables.V_FULLNAME, account.getFullName(),
                        MsgTemplateVariables.V_TENANT_NAME, account.getTenant(),
                        //Specific vars
                        MsgTemplateVariables.V_OTP_CODE, otpCode,
                        MsgTemplateVariables.V_OTP_LIFETIME_IN_M, String.valueOf(lifetime))))
                .build();
        //Send the message
        msgService.sendMessage(tenant, mailMessageDto, appProperties.isSendAsyncEmail());
    }

    void sendAccountCreatedEmail(String senderTenant, Account account, String key) throws JsonProcessingException {

    }

    public void sendPasswordRenewEmail(String senderTenant, Account account, @NotNull String key) {
    }
}

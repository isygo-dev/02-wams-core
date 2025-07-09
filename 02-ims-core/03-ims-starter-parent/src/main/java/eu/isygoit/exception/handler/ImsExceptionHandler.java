package eu.isygoit.exception.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.isygoit.config.AppProperties;
import eu.isygoit.constants.TenantConstants;
import eu.isygoit.dto.data.MailMessageDto;
import eu.isygoit.enums.IEnumEmailTemplate;
import eu.isygoit.service.IAppParameterService;
import eu.isygoit.service.IMsgService;
import eu.isygoit.types.EmailSubjects;
import eu.isygoit.types.MsgTemplateVariables;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * The type Ims exception handler.
 */
@Slf4j
@Component
public class ImsExceptionHandler extends ControllerExceptionHandler {

    private final AppProperties appProperties;

    @Autowired
    private IMsgService msgService;
    @Autowired
    private IAppParameterService appParameterService;

    /**
     * Instantiates a new Ims exception handler.
     *
     * @param appProperties the app properties
     */
    public ImsExceptionHandler(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public void processUnmanagedException(String message) {
        //Email message error to system admin
        log.error(message);
        String techAdminEmail = appParameterService.getTechnicalAdminEmail();
        if (StringUtils.hasText(techAdminEmail)) {
            try {
                MailMessageDto mailMessageDto = MailMessageDto.builder()
                        .subject(EmailSubjects.UNMANAGED_EXCEPTION)
                        .tenant(TenantConstants.DEFAULT_TENANT_NAME)
                        .toAddr(techAdminEmail)
                        .templateName(IEnumEmailTemplate.Types.UNMANAGED_EXCEPTION_TEMPLATE)
                        .variables(MailMessageDto.getVariablesAsString(Map.of(
                                //Common vars
                                MsgTemplateVariables.V_EXCEPTION, message
                        )))
                        .build();
                //Send the email message
                msgService.sendMessage(TenantConstants.SUPER_TENANT_NAME, mailMessageDto, appProperties.isSendAsyncEmail());
            } catch (JsonProcessingException e) {
                log.error("<Error>: send unmanaged exception email : {} ", e);
            }
        } else {
            log.error("<Error>: technical email not found");
        }
    }
}

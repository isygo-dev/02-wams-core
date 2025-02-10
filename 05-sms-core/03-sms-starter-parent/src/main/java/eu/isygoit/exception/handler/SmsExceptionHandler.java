package eu.isygoit.exception.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.isygoit.api.AppParameterControllerApi;
import eu.isygoit.config.AppProperties;
import eu.isygoit.constants.DomainConstants;
import eu.isygoit.dto.data.MailMessageDto;
import eu.isygoit.enums.IEnumMsgTemplateName;
import eu.isygoit.i18n.service.LocaleService;
import eu.isygoit.service.IMsgService;
import eu.isygoit.types.EmailSubjects;
import eu.isygoit.types.MsgTemplateVariables;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Optional;

/**
 * The type Sms exception handler.
 */
@Slf4j
@Component
public class SmsExceptionHandler extends ControllerExceptionHandler {

    private final LocaleService localeService;
    private final AppProperties appProperties;
    private final IMsgService msgService;
    private final AppParameterControllerApi appParameterService;
    @Autowired
    public SmsExceptionHandler(LocaleService localeService, AppProperties appProperties, IMsgService msgService, AppParameterControllerApi appParameterService) {
        this.localeService = localeService;
        this.appProperties = appProperties;
        this.msgService = msgService;
        this.appParameterService = appParameterService;
    }

    @Override
    protected LocaleService getLocaleServiceInstance() {
        return localeService;
    }

    @Override
    public void processUnmanagedException(String message) {
        log.error(message);

        try {
            var result = appParameterService.getTechnicalAdminEmail();

            Optional.ofNullable(result)
                    .filter(res -> res.getStatusCode().is2xxSuccessful() && res.hasBody() && StringUtils.hasText(res.getBody()))
                    .map(ResponseEntity::getBody)
                    .ifPresentOrElse(
                            techAdminEmail -> sendUnmanagedExceptionEmail(techAdminEmail, message),
                            () -> log.error("<Error>: technical email not found")
                    );
        } catch (Exception e) {
            log.error("Remote Feign call failed: ", e);
        }
    }

    private void sendUnmanagedExceptionEmail(String techAdminEmail, String message) {
        try {
            var mailMessageDto = MailMessageDto.builder()
                    .subject(EmailSubjects.UNMANAGED_EXCEPTION)
                    .domain(DomainConstants.DEFAULT_DOMAIN_NAME)
                    .toAddr(techAdminEmail)
                    .templateName(IEnumMsgTemplateName.Types.UNMANAGED_EXCEPTION_TEMPLATE)
                    .variables(MailMessageDto.getVariablesAsString(Map.of(
                            MsgTemplateVariables.V_EXCEPTION, message
                    )))
                    .build();

            msgService.sendMessage(DomainConstants.SUPER_DOMAIN_NAME, mailMessageDto, appProperties.isSendAsyncEmail());
        } catch (JsonProcessingException e) {
            log.error("<Error>: sending unmanaged exception email: ", e);
        }
    }
}

package eu.isygoit.controller;

import eu.isygoit.annotation.InjectMapperAndService;
import eu.isygoit.api.MailMessageServiceApi;
import eu.isygoit.com.rest.controller.ResponseFactory;
import eu.isygoit.com.rest.controller.constants.CtrlConstants;
import eu.isygoit.com.rest.controller.impl.tenancy.MappedCrudTenantController;
import eu.isygoit.dto.data.MailMessageDto;
import eu.isygoit.dto.data.MailOptionsDto;
import eu.isygoit.dto.data.MessageCompositionDto;
import eu.isygoit.enums.IEnumEmailTemplate;
import eu.isygoit.exception.handler.MmsExceptionHandler;
import eu.isygoit.mapper.MailMessageMapper;
import eu.isygoit.model.MailMessage;
import eu.isygoit.service.IMsgTemplateService;
import eu.isygoit.service.impl.MailMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;


/**
 * The type Mail message controller.
 */
//http://localhost:8060/webjars/swagger-ui/index.html#/
//http://localhost:8060/messaging/mms/private/mail
@Slf4j
@Validated
@RestController
@InjectMapperAndService(handler = MmsExceptionHandler.class, mapper = MailMessageMapper.class, minMapper = MailMessageMapper.class, service = MailMessageService.class)
@RequestMapping(path = "/api/v1/private/mail")
public class MailMessageController extends MappedCrudTenantController<UUID, MailMessage, MailMessageDto, MailMessageDto, MailMessageService>
        implements MailMessageServiceApi {

    @Autowired
    private MailMessageService mailMessageService;

    @Autowired
    private MailMessageMapper mailMessageMapper;

    @Autowired
    private IMsgTemplateService templateService;

    public ResponseEntity<?> sendMail(IEnumEmailTemplate.Types templateType, MailMessageDto mailMessage) {
        try {
            // Get tenant once and reuse
            var senderTenant = mailMessage.getSenderTenant() != null
                    ? mailMessage.getSenderTenant()
                    : requestContextService().getCurrentContext().getSenderTenant();

            if (templateType != null) {
                MessageCompositionDto messageComposition = templateService.composeMessageBody(
                        senderTenant,
                        templateType,
                        mailMessage.getVariablesAsMap(mailMessage.getVariables())
                );

                mailMessage.setBody(messageComposition.getContent());
                if (StringUtils.hasText(messageComposition.getDefaultSender())) {
                    mailMessage.setFromAddr(messageComposition.getDefaultSender());
                }
                if (messageComposition.getSenderConfigId() != null) {
                    mailMessage.setSenderConfigId(messageComposition.getSenderConfigId());
                }
            } else if (!StringUtils.hasText(mailMessage.getBody())) {
                mailMessage.setBody(mailMessage.getVariables());
            }

            mailMessageService.sendMail(
                    senderTenant,
                    templateType,
                    mailMessageMapper.dtoToEntity(mailMessage)
                    , MailOptionsDto.builder()
                            .returnDelivered(mailMessage.isReturnDelivered())
                            .returnRead(mailMessage.isReturnRead())
                            .build()
                    //convert multipart file list to resources
                    , mailMessageService.multiPartFileToResource(
                            senderTenant,
                            mailMessage.getResources())
            );

            return ResponseFactory.responseOk();
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }
}

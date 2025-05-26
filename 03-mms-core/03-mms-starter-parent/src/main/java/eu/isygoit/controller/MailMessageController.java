package eu.isygoit.controller;

import eu.isygoit.annotation.CtrlDef;
import eu.isygoit.api.MailMessageControllerApi;
import eu.isygoit.com.rest.controller.ResponseFactory;
import eu.isygoit.com.rest.controller.constants.CtrlConstants;
import eu.isygoit.com.rest.controller.impl.MappedCrudController;
import eu.isygoit.dto.data.MailMessageDto;
import eu.isygoit.dto.data.MailOptionsDto;
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
//http://localhost:8060/messaging/mms/private/account
@Slf4j
@Validated
@RestController
@CtrlDef(handler = MmsExceptionHandler.class, mapper = MailMessageMapper.class, minMapper = MailMessageMapper.class, service = MailMessageService.class)
@RequestMapping(path = "/api/v1/private/mail")
public class MailMessageController extends MappedCrudController<UUID, MailMessage, MailMessageDto, MailMessageDto, MailMessageService>
        implements MailMessageControllerApi {

    @Autowired
    private MailMessageService mailMessageService;

    @Autowired
    private MailMessageMapper mailMessageMapper;

    @Autowired
    private IMsgTemplateService templateService;

    public ResponseEntity<?> sendMail(String senderDomainName, IEnumEmailTemplate.Types template, MailMessageDto mailMessage) {
        try {
            if (template != null) {
                String body = templateService.composeMessageBody(senderDomainName, template,
                        mailMessage.getVariablesAsMap(mailMessage.getVariables()));
                mailMessage.setBody(body);
            } else if (!StringUtils.hasText(mailMessage.getBody())) {
                mailMessage.setBody(mailMessage.getVariables());
            }
            mailMessageService.sendMail(senderDomainName,
                    mailMessageMapper.dtoToEntity(mailMessage)
                    , MailOptionsDto.builder()
                            .returnDelivered(mailMessage.isReturnDelivered())
                            .returnRead(mailMessage.isReturnRead())
                            .build()
                    , mailMessageService.multiPartFileToResource(senderDomainName, mailMessage.getResources())); //convert multipart file list to resources
            return ResponseFactory.responseOk();
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }
}

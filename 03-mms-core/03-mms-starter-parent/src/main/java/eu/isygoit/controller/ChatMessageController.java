package eu.isygoit.controller;

import eu.isygoit.annotation.CtrlDef;
import eu.isygoit.api.ChatMessageControllerApi;
import eu.isygoit.app.ApplicationContextService;
import eu.isygoit.com.rest.controller.ResponseFactory;
import eu.isygoit.com.rest.controller.constants.CtrlConstants;
import eu.isygoit.com.rest.controller.impl.CrudControllerUtils;
import eu.isygoit.dto.common.RequestContextDto;
import eu.isygoit.dto.data.ChatAccountDto;
import eu.isygoit.dto.data.ChatMessageDto;
import eu.isygoit.dto.wsocket.WsConnectDto;
import eu.isygoit.exception.handler.MmsExceptionHandler;
import eu.isygoit.mapper.ChatMessageMapper;
import eu.isygoit.model.ChatMessage;
import eu.isygoit.service.impl.ChatMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The type Chat message controller.
 */
@Slf4j
@Validated
@RestController
@RequestMapping(path = "/api/v1/private/chat")
@CtrlDef(handler = MmsExceptionHandler.class, mapper = ChatMessageMapper.class, minMapper = ChatMessageMapper.class, service = ChatMessageService.class)
public class ChatMessageController extends CrudControllerUtils<ChatMessage, ChatMessageDto, ChatMessageDto, ChatMessageService>
        implements ChatMessageControllerApi {

    private final ApplicationContextService applicationContextService;

    public ChatMessageController(ApplicationContextService applicationContextService) {
        this.applicationContextService = applicationContextService;
    }

    @Override
    protected ApplicationContextService getApplicationContextServiceInstance() {
        return applicationContextService;
    }

    @Override
    public ResponseEntity<List<ChatMessageDto>> findByReceiverId(RequestContextDto requestContext,
                                                                 Long userId,
                                                                 Integer page,
                                                                 Integer size) {
        try {
            List<ChatMessage> list = crudService().findByReceiverId(userId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createDate")));
            if (CollectionUtils.isEmpty(list)) {
                return ResponseFactory.ResponseNoContent();
            }
            return ResponseFactory.ResponseOk(mapper().listEntityToDto(list));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<List<ChatMessageDto>> findByReceiverIdAndSenderId(RequestContextDto requestContext,
                                                                            Long userId,
                                                                            Long SenderId,
                                                                            Integer page,
                                                                            Integer size) {
        try {
            List<ChatMessage> list = crudService().findByReceiverIdAndSenderId(userId, SenderId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createDate")));
            if (CollectionUtils.isEmpty(list)) {
                return ResponseFactory.ResponseNoContent();
            }
            return ResponseFactory.ResponseOk(mapper().listEntityToDto(list));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<List<ChatAccountDto>> getChatAccounts(RequestContextDto requestContext,
                                                                Long userId,
                                                                Integer page,
                                                                Integer size) {
        try {
            List<ChatAccountDto> list = crudService().getChatAccounts(userId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createDate")));
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
    public ResponseEntity<List<WsConnectDto>> getChatStatus(RequestContextDto requestContext,
                                                            Long domainId) {
        try {
            List<WsConnectDto> list = crudService().getConnectionsByDomain(domainId);
            if (CollectionUtils.isEmpty(list)) {
                return ResponseFactory.ResponseNoContent();
            }
            return ResponseFactory.ResponseOk(list);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }
}

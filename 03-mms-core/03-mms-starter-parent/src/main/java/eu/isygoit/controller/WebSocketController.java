package eu.isygoit.controller;

import eu.isygoit.annotation.CtrlHandler;
import eu.isygoit.api.WebSocketControllerApi;
import eu.isygoit.app.ApplicationContextService;
import eu.isygoit.com.rest.controller.ResponseFactory;
import eu.isygoit.com.rest.controller.impl.ControllerExceptionHandler;
import eu.isygoit.dto.common.RequestContextDto;
import eu.isygoit.dto.wsocket.WsMessageWrapperDto;
import eu.isygoit.exception.handler.MmsExceptionHandler;
import eu.isygoit.service.IWebSocketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The type Web socket controller.
 */
@Slf4j
@Validated
@RestController
@CtrlHandler(MmsExceptionHandler.class)
@RequestMapping(path = "/api/v1/private/ws")
public class WebSocketController extends ControllerExceptionHandler implements WebSocketControllerApi {

    private final ApplicationContextService applicationContextService;
    private final IWebSocketService webSocketService;

    @Autowired
    public WebSocketController(ApplicationContextService applicationContextService, IWebSocketService webSocketService) {
        this.applicationContextService = applicationContextService;
        this.webSocketService = webSocketService;
    }

    @Override
    protected ApplicationContextService getApplicationContextServiceInstance() {
        return applicationContextService;
    }

    @Override
    public ResponseEntity<?> sendMessageToUser(RequestContextDto requestContext,
                                               Long recieverId, WsMessageWrapperDto message) {
        try {
            webSocketService.saveAndSendToUser(recieverId, message);
            return ResponseFactory.ResponseOk();
        } catch (Exception ex) {
            return getBackExceptionResponse(ex);
        }
    }

    @Override
    public ResponseEntity<?> sendMessageToGroup(RequestContextDto requestContext,
                                                Long groupId, WsMessageWrapperDto message) {
        try {
            webSocketService.saveAndSendToGroup(groupId, message);
            return ResponseFactory.ResponseOk();
        } catch (Exception ex) {
            return getBackExceptionResponse(ex);
        }
    }

    @Override
    public ResponseEntity<?> sendMessageToAll(RequestContextDto requestContext,
                                              WsMessageWrapperDto message) {
        try {
            webSocketService.saveAndSendToAll(message);
            return ResponseFactory.ResponseOk();
        } catch (Exception ex) {
            return getBackExceptionResponse(ex);
        }
    }
}

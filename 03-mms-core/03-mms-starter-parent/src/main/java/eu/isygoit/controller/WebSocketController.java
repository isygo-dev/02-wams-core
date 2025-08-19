package eu.isygoit.controller;

import eu.isygoit.annotation.InjectExceptionHandler;
import eu.isygoit.api.WebSocketControllerApi;
import eu.isygoit.com.rest.controller.ResponseFactory;
import eu.isygoit.com.rest.controller.impl.ControllerExceptionHandler;
import eu.isygoit.dto.common.ContextRequestDto;
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
@InjectExceptionHandler(MmsExceptionHandler.class)
@RequestMapping(path = "/api/v1/private/ws")
public class WebSocketController extends ControllerExceptionHandler implements WebSocketControllerApi {

    @Autowired
    private IWebSocketService webSocketService;

    @Override
    public ResponseEntity<?> sendMessageToUser(ContextRequestDto requestContext,
                                               Long recieverId, WsMessageWrapperDto message) {
        try {
            webSocketService.saveAndSendToUser(recieverId, message);
            return ResponseFactory.responseOk();
        } catch (Exception ex) {
            return getBackExceptionResponse(ex);
        }
    }

    @Override
    public ResponseEntity<?> sendMessageToGroup(ContextRequestDto requestContext,
                                                Long groupId, WsMessageWrapperDto message) {
        try {
            webSocketService.saveAndSendToGroup(groupId, message);
            return ResponseFactory.responseOk();
        } catch (Exception ex) {
            return getBackExceptionResponse(ex);
        }
    }

    @Override
    public ResponseEntity<?> sendMessageToAll(ContextRequestDto requestContext,
                                              WsMessageWrapperDto message) {
        try {
            webSocketService.saveAndSendToAll(message);
            return ResponseFactory.responseOk();
        } catch (Exception ex) {
            return getBackExceptionResponse(ex);
        }
    }
}

package eu.isygoit.api;

import eu.isygoit.constants.JwtConstants;
import eu.isygoit.constants.RestApiConstants;
import eu.isygoit.dto.common.RequestContextDto;
import eu.isygoit.dto.data.ChatAccountDto;
import eu.isygoit.dto.data.ChatMessageDto;
import eu.isygoit.dto.wsocket.WsConnectDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * The interface Chat message controller api.
 */
public interface ChatMessageControllerApi {

    /**
     * Find by receiver id response entity.
     *
     * @param requestContext the request context
     * @param userId         the user id
     * @param page           the page
     * @param size           the size
     * @return the response entity
     */
    @Operation(summary = "Find messages by receiver Id Api",
            description = "Find messages by receiver Id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = ChatMessageDto.class))})
    })
    @GetMapping(path = "/user")
    ResponseEntity<List<ChatMessageDto>> findByReceiverId(@RequestAttribute(value = JwtConstants.JWT_USER_CONTEXT, required = false) RequestContextDto requestContext,
                                                          @RequestParam(name = RestApiConstants.USER_ID) Long userId,
                                                          @RequestParam(name = RestApiConstants.PAGE) Integer page,
                                                          @RequestParam(name = RestApiConstants.SIZE) Integer size);

    /**
     * Find by receiver id and sender id response entity.
     *
     * @param requestContext the request context
     * @param userId         the user id
     * @param SenderId       the sender id
     * @param page           the page
     * @param size           the size
     * @return the response entity
     */
    @Operation(summary = "Find messages by receiver Id and sender Id Api",
            description = "Find messages by receiver Id and sender Id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = ChatMessageDto.class))})
    })
    @GetMapping(path = "/from")
    ResponseEntity<List<ChatMessageDto>> findByReceiverIdAndSenderId(@RequestAttribute(value = JwtConstants.JWT_USER_CONTEXT, required = false) RequestContextDto requestContext,
                                                                     @RequestParam(name = RestApiConstants.USER_ID) Long userId,
                                                                     @RequestParam(name = RestApiConstants.SENDER_ID) Long SenderId,
                                                                     @RequestParam(name = RestApiConstants.PAGE) Integer page,
                                                                     @RequestParam(name = RestApiConstants.SIZE) Integer size);

    /**
     * Gets chat accounts.
     *
     * @param requestContext the request context
     * @param userId         the user id
     * @param page           the page
     * @param size           the size
     * @return the chat accounts
     */
    @Operation(summary = "Get available chat accounts Api",
            description = "Get available chat accounts")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = ChatAccountDto.class))})
    })
    @GetMapping(path = "/account")
    ResponseEntity<List<ChatAccountDto>> getChatAccounts(@RequestAttribute(value = JwtConstants.JWT_USER_CONTEXT, required = false) RequestContextDto requestContext,
                                                         @RequestParam(name = RestApiConstants.USER_ID) Long userId,
                                                         @RequestParam(name = RestApiConstants.PAGE) Integer page,
                                                         @RequestParam(name = RestApiConstants.SIZE) Integer size);

    /**
     * Gets chat status.
     *
     * @param requestContext the request context
     * @param domainId       the domain id
     * @return the chat status
     */
    @Operation(summary = "Get chat accounts status Api",
            description = "Get chat accounts status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = WsConnectDto.class))})
    })
    @GetMapping(path = "/status/domain")
    ResponseEntity<List<WsConnectDto>> getChatStatus(@RequestAttribute(value = JwtConstants.JWT_USER_CONTEXT, required = false) RequestContextDto requestContext,
                                                     @RequestParam(name = RestApiConstants.DOMAIN_ID) Long domainId);
}

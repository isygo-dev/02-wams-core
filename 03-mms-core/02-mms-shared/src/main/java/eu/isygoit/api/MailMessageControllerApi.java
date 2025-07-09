package eu.isygoit.api;

import eu.isygoit.constants.RestApiConstants;
import eu.isygoit.dto.data.MailMessageDto;
import eu.isygoit.dto.extendable.IdAssignableDto;
import eu.isygoit.enums.IEnumEmailTemplate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;


/**
 * The interface Mail message controller api.
 */
public interface MailMessageControllerApi {

    /**
     * Send mail response entity.
     *
     * @param senderDomainName the sender tenant name
     * @param template         the template
     * @param mailMessage      the mail message
     * @return the response entity
     */
    @Operation(summary = "sendMail Api",
            description = "sendMail")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = IdAssignableDto.class))})
    })
    @PostMapping(path = "/send/{senderDomainName}/{template}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    ResponseEntity<?> sendMail(@PathVariable(name = RestApiConstants.SENDER_TENANT_NAME) String senderDomainName,
                               @PathVariable(name = RestApiConstants.TEMPLATE) IEnumEmailTemplate.Types template,
                               @ModelAttribute(name = RestApiConstants.mailMessage) MailMessageDto mailMessage);
}

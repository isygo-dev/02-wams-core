package eu.isygoit.remote.mms;

import eu.isygoit.api.MsgTemplateServiceApi;
import eu.isygoit.config.FeignConfig;
import eu.isygoit.dto.extendable.IdAssignableDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * The type Template controller.
 */
@FeignClient(configuration = FeignConfig.class, name = "messaging-service", contextId = "msg-template-config", path = "/api/v1/private/mail/template")
public interface MsgTemplateService extends MsgTemplateServiceApi {

    /**
     * Get template names response entity.
     *
     * @return the response entity
     */
    @Operation(summary = "get Template Names Api",
            description = "get Template Names")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = IdAssignableDto.class))})
    })
    @GetMapping(path = "/names")
    public ResponseEntity<List<String>> getTemplateNames();
}

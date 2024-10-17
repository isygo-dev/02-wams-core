package eu.isygoit.api;

import eu.isygoit.constants.RestApiConstants;
import eu.isygoit.dto.data.DomainDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * The interface Public controller api.
 */
public interface PublicControllerApi {

    /**
     * Gets domain by name.
     *
     * @param domain the domain
     * @return the domain by name
     */
    @Operation(summary = "Get domain by name Api",
            description = "Get domain by name")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = DomainDto.class))})
    })
    @GetMapping(path = "/domain/{name}")
    ResponseEntity<DomainDto> getDomainByName(@RequestParam(name = RestApiConstants.NAME) String domain);
}

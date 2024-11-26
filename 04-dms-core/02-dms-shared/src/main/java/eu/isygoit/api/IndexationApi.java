package eu.isygoit.api;

import eu.isygoit.constants.JwtConstants;
import eu.isygoit.dto.common.RequestContextDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;


/**
 * The interface Indexation api.
 */
public interface IndexationApi {

    /**
     * Calc keys occurrences response entity.
     *
     * @param requestContext the request context
     * @param keys           the keys
     * @param file           the file
     * @return the response entity
     */
    @Operation(summary = "calcKeysOccurrences Api",
            description = "calcKeysOccurrences")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = Map.class))})
    })
    @PostMapping(path = "/keys/occurrences", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<Map<String, Integer>> calcKeysOccurrences(@RequestAttribute(value = JwtConstants.JWT_USER_CONTEXT, required = false) RequestContextDto requestContext,
                                                             @RequestAttribute("keys") String[] keys,
                                                             @Valid @RequestBody MultipartFile file);
}

package eu.isygoit.api;

import eu.isygoit.com.rest.api.IMappedCrudApi;
import eu.isygoit.constants.JwtConstants;
import eu.isygoit.constants.RestApiConstants;
import eu.isygoit.dto.common.RequestContextDto;
import eu.isygoit.dto.data.AnnexDto;
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
 * The interface Annex controller api.
 */
public interface AnnexControllerApi extends IMappedCrudApi<Long, AnnexDto, AnnexDto> {

    /**
     * Gets annex by code.
     *
     * @param requestContext the request context
     * @param code           the code
     * @return the annex by code
     */
    @Operation(summary = "Get Annex by code Api",
            description = "Get Annex by code")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = AnnexDto.class))})
    })
    @GetMapping(path = "/byCode")
    ResponseEntity<List<AnnexDto>> getAnnexByTableCode(@RequestAttribute(value = JwtConstants.JWT_USER_CONTEXT, required = false) RequestContextDto requestContext,
                                                       @RequestParam(name = RestApiConstants.CODE) String code);

    @Operation(summary = "Get Annex by code and reference Api",
            description = "Get Annex by code and reference")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = AnnexDto.class))})
    })
    @GetMapping(path = "/byCodeAndRef")
    ResponseEntity<List<AnnexDto>> getAnnexByTableCodeAndReference(@RequestAttribute(value = JwtConstants.JWT_USER_CONTEXT, required = false) RequestContextDto requestContext,
                                                                   @RequestParam(name = RestApiConstants.CODE) String code,
                                                                   @RequestParam(name = RestApiConstants.REFERENCE) String reference);
}

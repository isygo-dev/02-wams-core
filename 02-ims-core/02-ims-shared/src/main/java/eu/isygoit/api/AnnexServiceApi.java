package eu.isygoit.api;

import eu.isygoit.com.rest.api.IMappedCrudApi;
import eu.isygoit.constants.RestApiConstants;
import eu.isygoit.dto.data.AnnexDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * The interface Annex controller api.
 */
public interface AnnexServiceApi extends IMappedCrudApi<Long, AnnexDto, AnnexDto> {

    /**
     * Gets annex by code.
     *
     * @param code the code
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
    ResponseEntity<List<AnnexDto>> getAnnexByTableCode(
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
    ResponseEntity<List<AnnexDto>> getAnnexByTableCodeAndReference(
            @RequestParam(name = RestApiConstants.CODE) String code,
            @RequestParam(name = RestApiConstants.REFERENCE) String reference);

    /**
     * Returns every distinct {@code tableCode} stored for the current tenant,
     * sorted alphabetically. Feeds {@code tableCode} filter pickers on the UI
     * (e.g. {@code AnnexesPanel}) so the user selects an existing code rather
     * than typing it.
     *
     * @return the ordered list of distinct table codes
     */
    @Operation(summary = "Get distinct annex table codes",
            description = "Get every distinct tableCode stored for the tenant, sorted alphabetically")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = String.class)))}),
            @ApiResponse(responseCode = "204", description = "No table code stored yet")
    })
    @GetMapping(path = "/distinctTableCodes")
    ResponseEntity<List<String>> getDistinctTableCodes();
}
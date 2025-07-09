package eu.isygoit.api;

import eu.isygoit.com.rest.api.IMappedCrudApi;
import eu.isygoit.constants.JwtConstants;
import eu.isygoit.constants.RestApiConstants;
import eu.isygoit.dto.common.RequestContextDto;
import eu.isygoit.dto.data.DomainDto;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;


/**
 * The interface Domain controller api.
 */
public interface DomainControllerApi extends IMappedCrudApi<Long, DomainDto, DomainDto> {

    /**
     * Update admin status response entity.
     *
     * @param requestContext the request context
     * @param id             the id
     * @param newStatus      the new status
     * @return the response entity
     */
    @Operation(summary = "Update tenant status Api",
            description = "Update tenant status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = DomainDto.class))})
    })
    @PutMapping(path = "/update-status")
    ResponseEntity<DomainDto> updateAdminStatus(@RequestAttribute(value = JwtConstants.JWT_USER_CONTEXT, required = false) RequestContextDto requestContext,
                                                @RequestParam(name = RestApiConstants.ID) Long id,
                                                @RequestParam(name = RestApiConstants.NEW_STATUS) IEnumEnabledBinaryStatus.Types newStatus);

    /**
     * Gets all tenant names.
     *
     * @param requestContext the request context
     * @return the all tenant names
     */
    @Operation(summary = "Get all tenant names Api",
            description = "Get all tenant names")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = String.class))})
    })
    @GetMapping(path = "/names")
    ResponseEntity<List<String>> getAllDomainNames(@RequestAttribute(value = JwtConstants.JWT_USER_CONTEXT, required = false) RequestContextDto requestContext);

    /**
     * Gets by name.
     *
     * @param requestContext the request context
     * @return the by name
     */
    @Operation(summary = "Get tenant by name Api",
            description = "Get tenant by name")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = DomainDto.class))})
    })
    @GetMapping(path = "/name")
    ResponseEntity<DomainDto> getByName(@RequestAttribute(value = JwtConstants.JWT_USER_CONTEXT, required = false) RequestContextDto requestContext);

    @Operation(summary = "Update tenant social link Api",
            description = "Update tenant social link")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = DomainDto.class))})
    })
    @PutMapping(path = "/social")
    ResponseEntity<DomainDto> updateSocialLink(@RequestAttribute(value = JwtConstants.JWT_USER_CONTEXT, required = false) RequestContextDto requestContext,
                                               @RequestParam(name = RestApiConstants.ID) Long id,
                                               @RequestParam(name = RestApiConstants.SOCIAL) String social,
                                               @RequestParam(name = RestApiConstants.LINK) String link);
}

package eu.isygoit.api;

import eu.isygoit.com.rest.api.IMappedCrudApi;
import eu.isygoit.constants.JwtConstants;
import eu.isygoit.constants.RestApiConstants;
import eu.isygoit.dto.common.RequestContextDto;
import eu.isygoit.dto.data.KmsDomainDto;
import eu.isygoit.enums.IEnumBinaryStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


/**
 * The interface Kms domain controller api.
 */
public interface KmsDomainControllerApi extends IMappedCrudApi<Long, KmsDomainDto, KmsDomainDto> {

    /**
     * Update admin status response entity.
     *
     * @param requestContext the request context
     * @param domain         the domain
     * @param newStatus      the new status
     * @return the response entity
     */
    @Operation(summary = "updateAdminStatus Api",
            description = "updateAdminStatus")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = KmsDomainDto.class))})
    })
    @PutMapping(path = "/update-status")
    ResponseEntity<KmsDomainDto> updateAdminStatus(@RequestAttribute(value = JwtConstants.JWT_USER_CONTEXT, required = false) RequestContextDto requestContext,
                                                   @RequestParam(name = RestApiConstants.DOMAIN_NAME) String domain,
                                                   @RequestParam(name = RestApiConstants.NEW_STATUS) IEnumBinaryStatus.Types newStatus);

    /**
     * Update domain response entity.
     *
     * @param domain the domain
     * @return the response entity
     */
    @Operation(summary = "Update domain info Api",
            description = "Update domain info")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = Boolean.class))})
    })
    @PostMapping(path = "/update")
    ResponseEntity<KmsDomainDto> updateDomain(//@RequestAttribute(value = JwtConstants.JWT_USER_CONTEXT, required = false) RequestContextDto requestContext,
                                              @Valid @RequestBody KmsDomainDto domain);
}

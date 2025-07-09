package eu.isygoit.api;

import eu.isygoit.com.rest.api.IMappedCrudApi;
import eu.isygoit.constants.JwtConstants;
import eu.isygoit.constants.RestApiConstants;
import eu.isygoit.dto.common.RequestContextDto;
import eu.isygoit.dto.data.KmsDomainDto;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


/**
 * The interface Kms tenant controller api.
 */
public interface KmsDomainControllerApi extends IMappedCrudApi<Long, KmsDomainDto, KmsDomainDto> {

    /**
     * Update admin status response entity.
     *
     * @param requestContext the request context
     * @param tenant         the tenant
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
                                                   @RequestParam(name = RestApiConstants.TENANT_NAME) String tenant,
                                                   @RequestParam(name = RestApiConstants.NEW_STATUS) IEnumEnabledBinaryStatus.Types newStatus);

    /**
     * Update tenant response entity.
     *
     * @param tenant the tenant
     * @return the response entity
     */
    @Operation(summary = "Update tenant info Api",
            description = "Update tenant info")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = Boolean.class))})
    })
    @PostMapping(path = "/update")
    ResponseEntity<Boolean> updateDomain(//@RequestAttribute(value = JwtConstants.JWT_USER_CONTEXT, required = false) RequestContextDto requestContext,
                                         @Valid @RequestBody KmsDomainDto tenant);
}

package eu.isygoit.api;

import eu.isygoit.com.rest.api.IMappedCrudApi;
import eu.isygoit.constants.JwtConstants;
import eu.isygoit.constants.RestApiConstants;
import eu.isygoit.dto.common.RequestContextDto;
import eu.isygoit.dto.data.AppParameterDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * The interface App parameter controller api.
 */
public interface AppParameterControllerApi extends IMappedCrudApi<Long, AppParameterDto, AppParameterDto> {

    /**
     * Gets value by tenant and name.
     *
     * @param requestContext the request context
     * @param tenant         the tenant
     * @param name           the name
     * @param allowDefault   the allow default
     * @return the value by tenant and name
     */
    @Operation(summary = "getValueByTenantAndName Api",
            description = "getValueByTenantAndName")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = String.class))})
    })
    @GetMapping(path = "/value/byDomainAndName")
    ResponseEntity<String> getValueByTenantAndName(@RequestAttribute(value = JwtConstants.JWT_USER_CONTEXT, required = false) RequestContextDto requestContext,
                                                   @RequestParam(name = RestApiConstants.TENANT_NAME) String tenant,
                                                   @RequestParam(name = RestApiConstants.NAME) String name,
                                                   @RequestParam(name = RestApiConstants.ALLOW_DEFAULT) Boolean allowDefault,
                                                   @RequestParam(name = RestApiConstants.DEFAULT_VALUE) String defaultValue);

    @Operation(summary = "getValueByTenantAndName Api",
            description = "getValueByTenantAndName")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = String.class))})
    })
    @GetMapping(path = "/value/technical_admin_email")
    ResponseEntity<String> getTechnicalAdminEmail();
}

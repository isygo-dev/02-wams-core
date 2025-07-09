package eu.isygoit.api;

import eu.isygoit.constants.JwtConstants;
import eu.isygoit.constants.RestApiConstants;
import eu.isygoit.dto.common.RequestContextDto;
import eu.isygoit.dto.common.UserContextDto;
import eu.isygoit.dto.data.DomainDto;
import eu.isygoit.dto.request.AccountAuthTypeRequest;
import eu.isygoit.dto.request.AuthenticationRequestDto;
import eu.isygoit.dto.request.RegisteredUserDto;
import eu.isygoit.dto.response.AuthResponseDto;
import eu.isygoit.dto.response.UserAccountDto;
import eu.isygoit.dto.response.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * The interface Public auth controller api.
 */
public interface PublicAuthControllerApi {

    /**
     * Authenticate response entity.
     *
     * @param request        the request
     * @param response       the response
     * @param authRequestDto the auth request dto
     * @return the response entity
     */
    @Operation(summary = "authenticate Api",
            description = "authenticate")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponseDto.class))})
    })
    @PostMapping(path = "/authenticate")
    ResponseEntity<AuthResponseDto> authenticate(HttpServletRequest request,
                                                 HttpServletResponse response,
                                                 @Valid @RequestBody AuthenticationRequestDto authRequestDto);

    /**
     * Generate forgot pwd token response entity.
     *
     * @param userContextDto the user context dto
     * @return the response entity
     */
    @Operation(summary = "generateForgotPWDToken Api",
            description = "generateForgotPWDToken")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = Boolean.class))})
    })
    @PostMapping(path = "/password/forgotten")
    ResponseEntity<Boolean> generateForgotPWDToken(@Valid @RequestBody UserContextDto userContextDto);

    /**
     * Gets authentication type.
     *
     * @param accountAuthTypeRequest the switch auth type request
     * @return the authentication type
     */
    @Operation(summary = "Get Authentication Type Api",
            description = "Get Authentication Type")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserContext.class))})
    })
    @PostMapping(path = "/authType")
    ResponseEntity<UserContext> getAuthenticationType(@Valid @RequestBody AccountAuthTypeRequest accountAuthTypeRequest);

    @Operation(summary = "Get Available Email Accounts Api",
            description = "Get Available Email Accounts")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserAccountDto.class))})
    })
    @GetMapping(path = "/accounts")
    ResponseEntity<List<UserAccountDto>> getAvailableEmailAccounts(@RequestParam(name = RestApiConstants.EMAIL) String email);

    /**
     * Switch auth type response entity.
     *
     * @param accountAuthTypeRequest the switch auth type request
     * @return the response entity
     */
    @Operation(summary = "switchAuthType Api",
            description = "switchAuthType")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = Boolean.class))})
    })
    @PostMapping(path = "/updateAuthType")
    ResponseEntity<Boolean> switchAuthType(@RequestAttribute(value = JwtConstants.JWT_USER_CONTEXT, required = false) RequestContextDto requestContext,
                                           @Valid @RequestBody AccountAuthTypeRequest accountAuthTypeRequest);

    /**
     * Register new account response entity.
     *
     * @param registeredUserDto the register new account dto
     * @return the response entity
     */
    @Operation(summary = "registerNewAccount Api",
            description = "registerNewAccount")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = Boolean.class))})
    })

    @PostMapping(path = "/register")
    ResponseEntity<Boolean> registerUser(@Valid @RequestBody RegisteredUserDto registeredUserDto);

    /**
     * Gets tenant by name.
     *
     * @param tenant the tenant
     * @return the tenant by name
     */
    @Operation(summary = "getTenantByName Api",
            description = "getTenantByName")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = DomainDto.class))})
    })
    @GetMapping(path = "/tenant")
    ResponseEntity<DomainDto> getTenantByName(@RequestParam(name = RestApiConstants.TENANT_NAME) String tenant);
}

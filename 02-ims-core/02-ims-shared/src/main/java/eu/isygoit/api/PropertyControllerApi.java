package eu.isygoit.api;

import eu.isygoit.constants.JwtConstants;
import eu.isygoit.constants.RestApiConstants;
import eu.isygoit.dto.common.ContextRequestDto;
import eu.isygoit.dto.data.PropertyDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * The interface Property controller api.
 */
public interface PropertyControllerApi {

    /**
     * Gets property by account.
     *
     * @param requestContext the request context
     * @param accountCode    the account code
     * @param guiName        the gui name
     * @param name           the name
     * @return the property by account
     */
    @Operation(summary = "Get property by account Api",
            description = "Get property by account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = PropertyDto.class))})
    })
    @GetMapping(path = "/account")
    ResponseEntity<PropertyDto> getPropertyByAccount(@RequestAttribute(value = JwtConstants.JWT_USER_CONTEXT, required = false) ContextRequestDto requestContext,
                                                     @RequestParam(name = RestApiConstants.ACCOUNT_CODE) String accountCode,
                                                     @RequestParam(name = RestApiConstants.GUI_NAME) String guiName,
                                                     @RequestParam(name = RestApiConstants.NAME) String name);

    /**
     * Gets property by account and gui.
     *
     * @param requestContext the request context
     * @param accountCode    the account code
     * @param guiName        the gui name
     * @return the property by account and gui
     */
    @Operation(summary = "Get property by account and Gui name Api",
            description = "Get property by account and Gui name")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = PropertyDto.class))})
    })
    @GetMapping(path = "/account/all")
    ResponseEntity<List<PropertyDto>> getPropertyByAccountAndGui(@RequestAttribute(value = JwtConstants.JWT_USER_CONTEXT, required = false) ContextRequestDto requestContext,
                                                                 @RequestParam(name = RestApiConstants.ACCOUNT_CODE) String accountCode,
                                                                 @RequestParam(name = RestApiConstants.GUI_NAME) String guiName);

    /**
     * Update property account response entity.
     *
     * @param requestContext the request context
     * @param accountCode    the account code
     * @param property       the property
     * @return the response entity
     */
    @Operation(summary = "Update property for account Api",
            description = "Update property for account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = PropertyDto.class))})
    })
    @PutMapping(path = "/account")
    ResponseEntity<PropertyDto> updatePropertyAccount(@RequestAttribute(value = JwtConstants.JWT_USER_CONTEXT, required = false) ContextRequestDto requestContext,
                                                      @RequestParam(name = RestApiConstants.CODE) String accountCode,
                                                      @Valid @RequestBody PropertyDto property);
}

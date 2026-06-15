package eu.isygoit.api;

import eu.isygoit.com.rest.api.IMappedCrudApi;
import eu.isygoit.constants.RestApiConstants;
import eu.isygoit.dto.data.CustomerDto;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * The type Customer controller.
 */
public interface CustomerServiceApi extends IMappedCrudApi<Long, CustomerDto, CustomerDto> {

    /**
     * Update customer status response entity.
     *
     * @param id        the id
     * @param newStatus the new status
     * @return the response entity
     */
    @Operation(summary = "Update customer status Api",
            description = "Update customer status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = CustomerDto.class))})
    })
    @PutMapping(path = "/update-status")
    public ResponseEntity<CustomerDto> updateCustomerStatus(
            @RequestParam(name = RestApiConstants.ID) Long id,
            @RequestParam(name = RestApiConstants.NEW_STATUS) IEnumEnabledBinaryStatus.Types newStatus);

    /**
     * Link to existing account response entity.
     *
     * @param id          the id
     * @param accountCode the account code
     * @return the response entity
     */
    @Operation(summary = "Link customer to existing account Api",
            description = "Link customer to existing account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = CustomerDto.class))})
    })
    @PutMapping(path = "/link-account")
    public ResponseEntity<CustomerDto> LinkToExistingAccount(
            @RequestParam(name = RestApiConstants.ID) Long id,
            @RequestParam(name = RestApiConstants.ACCOUNT_CODE) String accountCode);

    /**
     * Gets name customer.
     *
     * @return the name customer
     */
    @Operation(summary = "Get customers name list Api",
            description = "Get customers name list")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = String.class))})
    })
    @GetMapping(path = "/names")
    public ResponseEntity<List<String>> getCustomersNames();
}

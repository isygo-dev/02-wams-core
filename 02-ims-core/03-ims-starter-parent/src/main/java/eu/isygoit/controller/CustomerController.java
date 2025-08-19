package eu.isygoit.controller;

import eu.isygoit.annotation.InjectMapperAndService;
import eu.isygoit.com.rest.controller.ResponseFactory;
import eu.isygoit.com.rest.controller.constants.CtrlConstants;
import eu.isygoit.com.rest.controller.impl.tenancy.MappedCrudTenantController;
import eu.isygoit.constants.JwtConstants;
import eu.isygoit.constants.RestApiConstants;
import eu.isygoit.dto.common.ContextRequestDto;
import eu.isygoit.dto.data.CustomerDto;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.exception.handler.ImsExceptionHandler;
import eu.isygoit.mapper.CustomerMapper;
import eu.isygoit.model.Customer;
import eu.isygoit.service.ICustomerService;
import eu.isygoit.service.impl.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * The type Customer controller.
 */
@Slf4j
@Validated
@RestController
@InjectMapperAndService(handler = ImsExceptionHandler.class, mapper = CustomerMapper.class, minMapper = CustomerMapper.class, service = CustomerService.class)
@RequestMapping(path = "/api/v1/private/customer")
public class CustomerController extends MappedCrudTenantController<Long, Customer, CustomerDto, CustomerDto, CustomerService> {

    @Autowired
    private ICustomerService customerService;

    /**
     * Update customer status response entity.
     *
     * @param requestContext the request context
     * @param id             the id
     * @param newStatus      the new status
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
    public ResponseEntity<CustomerDto> updateCustomerStatus(@RequestAttribute(value = JwtConstants.JWT_USER_CONTEXT) ContextRequestDto requestContext,
                                                            @RequestParam(name = RestApiConstants.ID) Long id,
                                                            @RequestParam(name = RestApiConstants.NEW_STATUS) IEnumEnabledBinaryStatus.Types newStatus) {
        log.info("in update status");
        try {
            return ResponseFactory.responseOk(mapper().entityToDto(crudService().updateStatus(id, newStatus)));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    /**
     * Link to existing account response entity.
     *
     * @param requestContext the request context
     * @param id             the id
     * @param accountCode    the account code
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
    public ResponseEntity<CustomerDto> LinkToExistingAccount(@RequestAttribute(value = JwtConstants.JWT_USER_CONTEXT) ContextRequestDto requestContext,
                                                             @RequestParam(name = RestApiConstants.ID) Long id,
                                                             @RequestParam(name = RestApiConstants.ACCOUNT_CODE) String accountCode) {
        log.info("Link to existing account");
        try {
            return ResponseFactory.responseOk(mapper().entityToDto(crudService().linkToAccount(requestContext.getSenderTenant(), id, accountCode)));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    /**
     * Gets name customer.
     *
     * @param requestContext the request context
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
    public ResponseEntity<List<String>> getCustomersNames(@RequestAttribute(value = JwtConstants.JWT_USER_CONTEXT) ContextRequestDto requestContext) {
        try {
            return ResponseFactory.responseOk(customerService.getNames());
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }
}

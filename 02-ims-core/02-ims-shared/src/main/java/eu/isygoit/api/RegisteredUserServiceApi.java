package eu.isygoit.api;

import eu.isygoit.com.rest.api.IMappedCrudApi;
import eu.isygoit.dto.data.AccountDto;
import eu.isygoit.dto.request.CreateAccountFromRegisteredRequestDto;
import eu.isygoit.dto.request.RegisteredUserDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * The interface Account controller api.
 */
public interface RegisteredUserServiceApi extends IMappedCrudApi<Long, RegisteredUserDto, RegisteredUserDto> {

    /**
     * Create an account from a registered user.
     *
     * @param request the account creation request
     * @return the created account response
     */
    @Operation(
            summary = "Create account from registered user",
            description = "Creates an account from an existing registered user. The registered user must have status NEW or CONFIRMED."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Account created successfully",
                    content = @Content(schema = @Schema(implementation = AccountDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request or user cannot be processed"),
            @ApiResponse(responseCode = "404", description = "Registered user not found"),
            @ApiResponse(responseCode = "409", description = "User already processed or account already exists")
    })
    @PostMapping("/create-account")
    ResponseEntity<AccountDto> createAccountFromRegistered(
            @Valid @RequestBody CreateAccountFromRegisteredRequestDto request
    );
}

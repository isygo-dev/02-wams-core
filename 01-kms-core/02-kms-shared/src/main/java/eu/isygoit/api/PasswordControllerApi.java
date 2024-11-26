package eu.isygoit.api;

import eu.isygoit.constants.JwtConstants;
import eu.isygoit.constants.RestApiConstants;
import eu.isygoit.dto.common.RequestContextDto;
import eu.isygoit.dto.common.ResetPwdViaTokenRequestDto;
import eu.isygoit.dto.request.*;
import eu.isygoit.dto.response.AccessTokenResponseDto;
import eu.isygoit.enums.IEnumAuth;
import eu.isygoit.enums.IEnumPasswordStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * The interface Password controller api.
 */
public interface PasswordControllerApi {

    /**
     * Generate response entity.
     *
     * @param type               the type
     * @param generatePwdRequest the generate pwd request
     * @return the response entity
     */
    @Operation(summary = "generate Api",
            description = "generate")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = Boolean.class))})
    })
    @PostMapping(path = "/generate/{type}")
    ResponseEntity<Integer> generate(//@RequestAttribute(value = JwtConstants.JWT_USER_CONTEXT, required = false) RequestContextDto requestContext,
                                     @Valid @PathVariable(name = RestApiConstants.type) IEnumAuth.Types authType,
                                     @Valid @RequestBody GeneratePwdRequestDto generatePwdRequest);

    /**
     * Reset password via token response entity.
     *
     * @param resetPwdViaTokenRequestDto the reset pwd via token request dto
     * @return the response entity
     */
    @Operation(summary = "resetPasswordViaToken Api",
            description = "resetPasswordViaToken")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = String.class))})
    })
    @PostMapping(path = "/reset-password")
    ResponseEntity<String> resetPasswordViaToken(//@RequestAttribute(value = JwtConstants.JWT_USER_CONTEXT, required = false) RequestContextDto requestContext,
                                                 @Valid @RequestBody ResetPwdViaTokenRequestDto resetPwdViaTokenRequestDto);

    /**
     * Change password response entity.
     *
     * @param requestContext the request context
     * @param oldPassword    the old password
     * @param newPassword    the new password
     * @return the response entity
     */
    @Operation(summary = "changePassword Api",
            description = "changePassword")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = String.class))})
    })
    @PostMapping(path = "/change-password")
    ResponseEntity<String> changePassword(@RequestAttribute(value = JwtConstants.JWT_USER_CONTEXT, required = false) RequestContextDto requestContext,
                                          @RequestParam(name = RestApiConstants.OLD_PASSWORD) String oldPassword,
                                          @RequestParam(name = RestApiConstants.NEW_PASSWORD) String newPassword);

    /**
     * Pattern check response entity.
     *
     * @param checkPwdRequest the check pwd request
     * @return the response entity
     */
    @Operation(summary = "patternCheck Api",
            description = "patternCheck")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = Boolean.class))})
    })
    @PostMapping(path = "/pattern/check")
    ResponseEntity<Boolean> patternCheck(//@RequestAttribute(value = JwtConstants.JWT_USER_CONTEXT, required = false) RequestContextDto requestContext,
                                         @Valid @RequestBody CheckPwdRequestDto checkPwdRequest);

    /**
     * Gets access.
     *
     * @param matchPwdRequest the match pwd request
     * @return the access
     */
    @Operation(summary = "getAccess Api",
            description = "getAccess")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = AccessTokenResponseDto.class))})
    })
    @PostMapping(path = "/access")
    ResponseEntity<AccessTokenResponseDto> getAccess(//@RequestAttribute(value = JwtConstants.JWT_USER_CONTEXT, required = false) RequestContextDto requestContext,
                                                     @Valid @RequestBody AccessRequestDto matchPwdRequest);

    /**
     * Matches response entity.
     *
     * @param matchesRequest the matches request
     * @return the response entity
     */
    @Operation(summary = "matches Api",
            description = "matches")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = IEnumPasswordStatus.Types.class))})
    })
    @PostMapping(path = "/matches")
    ResponseEntity<IEnumPasswordStatus.Types> matches(//@RequestAttribute(value = JwtConstants.JWT_USER_CONTEXT, required = false) RequestContextDto requestContext,
                                                      @Valid @RequestBody MatchesRequestDto matchesRequest);

    /**
     * Is password expired response entity.
     *
     * @param isPwdExpiredRequestDto the is pwd expired request dto
     * @return the response entity
     */
    @Operation(summary = "isPasswordExpired Api",
            description = "isPasswordExpired")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = Boolean.class))})
    })
    @PostMapping(path = "/isExpired")
    ResponseEntity<Boolean> isPasswordExpired(//@RequestAttribute(value = JwtConstants.JWT_USER_CONTEXT, required = false) RequestContextDto requestContext,
                                              @Valid @RequestBody IsPwdExpiredRequestDto isPwdExpiredRequestDto);

    /**
     * Update account response entity.
     *
     * @param account the account
     * @return the response entity
     */
    @Operation(summary = "Update account info Api",
            description = "Update account info")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = Boolean.class))})
    })
    @PostMapping(path = "/account")
    ResponseEntity<Boolean> updateAccount(//@RequestAttribute(value = JwtConstants.JWT_USER_CONTEXT, required = false) RequestContextDto requestContext,
                                          @Valid @RequestBody UpdateAccountRequestDto account);
}

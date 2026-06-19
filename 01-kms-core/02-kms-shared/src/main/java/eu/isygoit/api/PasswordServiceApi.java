package eu.isygoit.api;

import eu.isygoit.constants.RestApiConstants;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * The interface Password controller api.
 */
public interface PasswordServiceApi {

    /**
     * Generate response entity.
     *
     * @param generatePwdRequest the generate pwd request
     * @return the response entity
     */
    @Operation(summary = "generate Token Api",
            description = "generate Token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = Boolean.class))})
    })
    @PostMapping(path = "/generate/TOKEN")
    ResponseEntity<Integer> generateToken(
            @Valid @RequestBody GeneratePwdRequestDto generatePwdRequest);

    /**
     * Generate response entity.
     *
     * @param generatePwdRequest the generate pwd request
     * @return the response entity
     */
    @Operation(summary = "generate Password Api",
            description = "generate Password")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = Boolean.class))})
    })
    @PostMapping(path = "/generate/PWD")
    ResponseEntity<Integer> generatePwd(
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
    ResponseEntity<String> resetPasswordViaToken(
            @Valid @RequestBody ResetPwdViaTokenRequestDto resetPwdViaTokenRequestDto);

    /**
     * Change password response entity.
     *
     * @param oldPassword the old password
     * @param newPassword the new password
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
    ResponseEntity<String> changePassword(
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
    ResponseEntity<Boolean> patternCheck(
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
    ResponseEntity<AccessTokenResponseDto> getAccess(
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
    @PostMapping(path = "/matches/PWD")
    ResponseEntity<IEnumPasswordStatus.Types> matchesPasssword(
            @Valid @RequestBody MatchesRequestDto matchesRequest);

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
    @PostMapping(path = "/matches/TOKEN")
    ResponseEntity<IEnumPasswordStatus.Types> matchesToken(
            @Valid @RequestBody MatchesRequestDto matchesRequest);

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
    @PostMapping(path = "/matches/OTP")
    ResponseEntity<IEnumPasswordStatus.Types> matchesOtp(
            @Valid @RequestBody MatchesRequestDto matchesRequest);

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
    @PostMapping(path = "/matches/QRC")
    ResponseEntity<IEnumPasswordStatus.Types> matchesQrc(
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
    @PostMapping(path = "/isExpired/PWD")
    ResponseEntity<Boolean> isPwdExpired(
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
    ResponseEntity<Boolean> updateAccount(
            @Valid @RequestBody UpdateAccountRequestDto account);
}

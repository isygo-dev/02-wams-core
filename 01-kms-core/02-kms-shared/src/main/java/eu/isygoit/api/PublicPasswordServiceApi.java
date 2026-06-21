package eu.isygoit.api;

import eu.isygoit.dto.common.UserContextRequestDto;
import eu.isygoit.dto.request.AccessRequestDto;
import eu.isygoit.dto.request.GeneratePwdRequestDto;
import eu.isygoit.dto.request.IsPwdExpiredRequestDto;
import eu.isygoit.dto.request.MatchesRequestDto;
import eu.isygoit.dto.response.AccessTokenResponseDto;
import eu.isygoit.enums.IEnumPasswordStatus;
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
 * The interface Public password controller api.
 */
public interface PublicPasswordServiceApi {

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
    @PostMapping(path = "/isExpired/OTP")
    ResponseEntity<Boolean> isOtpExpired(
            @Valid @RequestBody IsPwdExpiredRequestDto isPwdExpiredRequestDto);

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
    @PostMapping(path = "/isExpired/QRC")
    ResponseEntity<Boolean> isQrcExpired(
            @Valid @RequestBody IsPwdExpiredRequestDto isPwdExpiredRequestDto);

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
     * Generate response entity.
     *
     * @param generatePwdRequest the generate pwd request
     * @return the response entity
     */
    @Operation(summary = "generate OTP Api",
            description = "generate OTP")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = Boolean.class))})
    })
    @PostMapping(path = "/generate/OTP")
    ResponseEntity<Integer> generateOtp(
            @Valid @RequestBody GeneratePwdRequestDto generatePwdRequest);

    /**
     * Generate response entity.
     *
     * @param generatePwdRequest the generate pwd request
     * @return the response entity
     */
    @Operation(summary = "generate QRC Api",
            description = "generate QRC")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = Boolean.class))})
    })
    @PostMapping(path = "/generate/QRC")
    ResponseEntity<Integer> generateQrc(
            @Valid @RequestBody GeneratePwdRequestDto generatePwdRequest);

    /**
     * Generate forgot password access token response entity.
     *
     * @param userContextDto the user context dto
     * @return the response entity
     */
    @Operation(summary = "generateForgotPasswordAccessToken Api",
            description = "generateForgotPasswordAccessToken")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = Boolean.class))})
    })
    @PostMapping(path = "/access-token")
    ResponseEntity<Boolean> generateForgotPasswordAccessToken(@Valid @RequestBody UserContextRequestDto userContextDto);
}

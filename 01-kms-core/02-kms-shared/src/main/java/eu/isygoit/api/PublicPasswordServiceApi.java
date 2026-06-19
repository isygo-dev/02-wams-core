package eu.isygoit.api;

import eu.isygoit.dto.common.UserContextRequestDto;
import eu.isygoit.dto.request.GeneratePwdRequestDto;
import eu.isygoit.dto.request.IsPwdExpiredRequestDto;
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

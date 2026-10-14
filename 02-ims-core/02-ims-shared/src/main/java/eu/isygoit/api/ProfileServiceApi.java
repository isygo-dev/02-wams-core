package eu.isygoit.api;

import eu.isygoit.dto.common.ChangePasswordRequestDto;
import eu.isygoit.dto.data.AccountDto;
import eu.isygoit.dto.data.AccountStatDto;
import eu.isygoit.dto.data.ConnectionPageDto;
import eu.isygoit.dto.data.DailyLoginCountDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Profile Service API interface.
 * Provides endpoints for user profile management operations.
 * Uses RequestContext to identify the authenticated user (tenant + username).
 */
@Tag(name = "Profile Management", description = "API for managing user profiles")
public interface ProfileServiceApi extends AuthTypeServiceApi {

    /**
     * Get current user profile.
     *
     * @return the account DTO
     */
    @Operation(summary = "Get current user profile",
            description = "Retrieves the complete profile information of the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile retrieved successfully",
                    content = @Content(schema = @Schema(implementation = AccountDto.class))),
            @ApiResponse(responseCode = "404", description = "Profile not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    ResponseEntity<AccountDto> getProfile();

    /**
     * Update user profile.
     *
     * @param accountDto the account DTO with updated information
     * @return the updated account DTO
     */
    @Operation(summary = "Update user profile",
            description = "Updates the profile information of the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile updated successfully",
                    content = @Content(schema = @Schema(implementation = AccountDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "404", description = "Profile not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PutMapping
    ResponseEntity<AccountDto> updateProfile(
            @Valid @RequestBody AccountDto accountDto);

    /**
     * Change user password.
     *
     * @param changePasswordRequest the change password request
     * @return the response entity
     */
    @Operation(summary = "Change user password",
            description = "Changes the password of the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password changed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid password or confirmation mismatch"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PostMapping("/change-password")
    ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequestDto changePasswordRequest);

    /**
     * Upload user avatar.
     *
     * @param file the avatar image file
     * @return the updated account DTO
     * @throws IOException the io exception
     */
    @Operation(summary = "Upload user avatar",
            description = "Uploads a new avatar image for the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Avatar uploaded successfully",
                    content = @Content(schema = @Schema(implementation = AccountDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid file format or size"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<AccountDto> uploadAvatar(
            @Parameter(description = "Avatar image file") @RequestParam("file") MultipartFile file) throws IOException;

    /**
     * Download user avatar.
     *
     * @return the resource
     * @throws IOException the io exception
     */
    @Operation(summary = "Download user avatar",
            description = "Downloads the avatar image of the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Avatar retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Avatar not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping(value = "/avatar", produces = MediaType.IMAGE_JPEG_VALUE)
    ResponseEntity<Resource> downloadAvatar() throws IOException;

    /**
     * Delete user avatar.
     *
     * @return the response entity
     */
    @Operation(summary = "Delete user avatar",
            description = "Deletes the avatar image of the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Avatar deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Avatar not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @DeleteMapping("/avatar")
    ResponseEntity<Void> deleteAvatar();

    /**
     * Get user statistics.
     *
     * @return the statistics
     */
    @Operation(summary = "Get user statistics",
            description = "Retrieves statistics for the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/statistics")
    ResponseEntity<AccountStatDto> getUserStatistics();

    /**
     * One page of the authenticated user's login history, newest first.
     *
     * @param page zero-based page index
     * @param size page size (the server caps it at 50)
     * @return the page
     */
    @Operation(summary = "Get login history page",
            description = "Retrieves one page of the authenticated user's tracked logins, newest first")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Page retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ConnectionPageDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/connections")
    ResponseEntity<ConnectionPageDto> getConnections(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size);

    /**
     * Logins per day over the last days (zero-filled), oldest first.
     *
     * @param days number of days including today (the server caps it at 90)
     * @return the daily counts
     */
    @Operation(summary = "Get daily login counts",
            description = "Retrieves the number of tracked logins per day for the last days, oldest first")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Counts retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/connections/activity")
    ResponseEntity<List<DailyLoginCountDto>> getLoginActivity(
            @RequestParam(name = "days", defaultValue = "30") int days);
}
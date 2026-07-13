package eu.isygoit.api;

import eu.isygoit.dto.common.ChangePasswordRequestDto;
import eu.isygoit.dto.data.AccountDto;
import eu.isygoit.dto.data.AccountStatDto;
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

/**
 * Profile Service API interface.
 * Provides endpoints for user profile management operations.
 * Uses RequestContext to identify the authenticated user (tenant + username).
 */
@Tag(name = "Profile Management", description = "API for managing user profiles")
public interface ProfileServiceApi {

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
}
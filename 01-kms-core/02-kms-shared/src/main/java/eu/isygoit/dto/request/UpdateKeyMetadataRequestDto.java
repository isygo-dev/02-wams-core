package eu.isygoit.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating key metadata (description and display name).
 * This request allows modification of non-cryptographic key attributes.
 * Does not affect the key material or cryptographic properties.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
        title = "Update Key Metadata Request",
        description = "Request payload for updating key metadata such as description and display name"
)
public class UpdateKeyMetadataRequestDto {

    /**
     * New description for the key.
     * Used to document the purpose and usage of the key.
     */
    @JsonProperty("description")
    @Schema(
            description = "Updated description for the key (max 8192 characters)",
            example = "Customer data encryption key for production",
            maxLength = 8192
    )
    private String description;

    /**
     * Display name for the key.
     * User-friendly name for identifying the key in UI and logs.
     */
    @JsonProperty("displayName")
    @Schema(
            description = "Display name for the key (max 256 characters)",
            example = "prod-customer-data-key",
            maxLength = 256
    )
    private String displayName;

    /**
     * Custom attribute 1 for key metadata.
     * Can be used to store application-specific metadata.
     */
    @JsonProperty("customAttribute1")
    @Schema(
            description = "Custom metadata attribute 1 (max 1024 characters)",
            example = "team:data-engineering",
            maxLength = 1024
    )
    private String customAttribute1;

    /**
     * Custom attribute 2 for key metadata.
     * Can be used to store application-specific metadata.
     */
    @JsonProperty("customAttribute2")
    @Schema(
            description = "Custom metadata attribute 2 (max 1024 characters)",
            example = "compliance:pci-dss",
            maxLength = 1024
    )
    private String customAttribute2;

    /**
     * Validates the request has at least one field to update.
     *
     * @return true if at least one field is set for update
     */
    public boolean hasUpdates() {
        return description != null || displayName != null ||
                customAttribute1 != null || customAttribute2 != null;
    }

    /**
     * Validates the request payload.
     *
     * @throws IllegalArgumentException if validation fails
     */
    public void validate() {
        if (!hasUpdates()) {
            throw new IllegalArgumentException("At least one metadata field must be provided for update");
        }

        if (description != null && description.length() > 8192) {
            throw new IllegalArgumentException("Description exceeds maximum length of 8192 characters");
        }

        if (displayName != null && displayName.length() > 256) {
            throw new IllegalArgumentException("Display name exceeds maximum length of 256 characters");
        }

        if (customAttribute1 != null && customAttribute1.length() > 1024) {
            throw new IllegalArgumentException("Custom attribute 1 exceeds maximum length of 1024 characters");
        }

        if (customAttribute2 != null && customAttribute2.length() > 1024) {
            throw new IllegalArgumentException("Custom attribute 2 exceeds maximum length of 1024 characters");
        }
    }
}
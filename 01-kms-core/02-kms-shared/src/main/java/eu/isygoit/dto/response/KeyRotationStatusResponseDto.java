package eu.isygoit.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO representing the key rotation status and configuration.
 * Contains information about whether automatic rotation is enabled
 * and when the last rotation occurred.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
        title = "Key Rotation Status",
        description = "Contains key rotation configuration and status information"
)
public class KeyRotationStatusResponseDto {

    /**
     * The ID of the key.
     */
    @JsonProperty("keyId")
    @Schema(
            description = "The unique identifier of the key",
            example = "550e8400-e29b-41d4-a716-446655440000",
            required = true
    )
    private Long keyId;

    /**
     * The ARN of the key.
     */
    @JsonProperty("keyArn")
    @Schema(
            description = "The Amazon Resource Name (ARN) of the key",
            example = "arn:aws:kms:us-east-1:123456789012:key/550e8400-e29b-41d4-a716-446655440000",
            required = true
    )
    private String keyArn;

    /**
     * Whether automatic key rotation is enabled.
     */
    @JsonProperty("rotationEnabled")
    @Schema(
            description = "Whether automatic annual key rotation is enabled",
            example = "true",
            required = true
    )
    private Boolean rotationEnabled;

    /**
     * The rotation period in days (typically 365 for annual rotation).
     */
    @JsonProperty("rotationPeriodDays")
    @Schema(
            description = "The rotation period in days (standard is 365 for annual rotation)",
            example = "365",
            required = true
    )
    private Integer rotationPeriodDays;

    /**
     * The date of the most recent key rotation.
     * Null if the key has never been rotated.
     */
    @JsonProperty("lastRotationDate")
    @Schema(
            description = "The date and time of the last key rotation (ISO 8601 format)",
            example = "2024-01-15T10:30:00Z",
            nullable = true
    )
    private LocalDateTime lastRotationDate;

    /**
     * The date of the next scheduled automatic rotation.
     * Null if automatic rotation is disabled.
     */
    @JsonProperty("nextRotationDate")
    @Schema(
            description = "The date and time of the next scheduled automatic rotation (ISO 8601 format)",
            example = "2025-01-15T10:30:00Z",
            nullable = true
    )
    private LocalDateTime nextRotationDate;

    /**
     * Number of versions (rotations) of the key.
     */
    @JsonProperty("versionCount")
    @Schema(
            description = "The total number of key versions (rotations) that exist",
            example = "5",
            required = true
    )
    private Integer versionCount;

    /**
     * The creation date of the key.
     */
    @JsonProperty("keyCreationDate")
    @Schema(
            description = "The date and time the key was created (ISO 8601 format)",
            example = "2020-06-10T08:15:00Z",
            required = true
    )
    private LocalDateTime keyCreationDate;

    /**
     * The current state of the key.
     */
    @JsonProperty("keyState")
    @Schema(
            description = "The current state of the key (Enabled, Disabled, PendingDeletion, etc.)",
            example = "Enabled",
            required = true,
            allowableValues = {"Enabled", "Disabled", "PendingDeletion", "PendingImport"}
    )
    private String keyState;

    /**
     * Whether the key is a customer managed key (vs AWS managed).
     */
    @JsonProperty("isCustomerManagedKey")
    @Schema(
            description = "Whether the key is customer managed (true) or AWS managed (false)",
            example = "true",
            required = true
    )
    private Boolean isCustomerManagedKey;

    /**
     * The reason rotation is not enabled (if applicable).
     * Only populated when rotationEnabled is false.
     */
    @JsonProperty("rotationDisabledReason")
    @Schema(
            description = "Reason why rotation is disabled (e.g., 'ManuallyDisabled', 'ImportedKeyMaterial')",
            example = "ManuallyDisabled",
            nullable = true
    )
    private String rotationDisabledReason;

    /**
     * Days until next automatic rotation.
     */
    @JsonProperty("daysUntilNextRotation")
    @Schema(
            description = "Days remaining until the next automatic rotation occurs (-1 if rotation is disabled)",
            example = "156",
            nullable = true
    )
    private Integer daysUntilNextRotation;

    /**
     * Last rotation initiated by principal (for auditing).
     */
    @JsonProperty("lastRotationInitiatedBy")
    @Schema(
            description = "The principal (user/role/service) that last initiated key rotation",
            example = "arn:aws:iam::123456789012:user/admin",
            nullable = true
    )
    private String lastRotationInitiatedBy;

    /**
     * Calculates days until next rotation.
     * Returns -1 if rotation is disabled or nextRotationDate is null.
     */
    public void calculateDaysUntilNextRotation() {
        if (rotationEnabled && nextRotationDate != null) {
            LocalDateTime now = LocalDateTime.now();
            this.daysUntilNextRotation = Math.toIntExact(
                    java.time.temporal.ChronoUnit.DAYS.between(now, nextRotationDate)
            );
        } else {
            this.daysUntilNextRotation = -1;
        }
    }

    /**
     * Determines if rotation is overdue (if enabled).
     */
    public boolean isRotationOverdue() {
        if (!rotationEnabled || nextRotationDate == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(nextRotationDate);
    }

    /**
     * Determines if a rotation happened recently (within N days).
     */
    public boolean rotatedWithinDays(int days) {
        if (lastRotationDate == null) {
            return false;
        }
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
        return lastRotationDate.isAfter(cutoffDate);
    }
}
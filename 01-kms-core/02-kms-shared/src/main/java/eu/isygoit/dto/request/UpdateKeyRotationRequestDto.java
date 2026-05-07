package eu.isygoit.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for enabling or disabling automatic key rotation.
 * When enabled, KMS automatically creates a new key version once per year.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
        title = "Update Key Rotation Request",
        description = "Request payload for enabling or disabling automatic key rotation"
)
public class UpdateKeyRotationRequestDto {

    /**
     * Whether to enable automatic key rotation.
     * True to enable annual automatic rotation, false to disable.
     */
    @JsonProperty("enableRotation")
    @Schema(
            description = "Set to true to enable automatic annual key rotation, false to disable",
            example = "true",
            required = true
    )
    private Boolean enableRotation;

    /**
     * Optional custom rotation period in days.
     * If null, uses the default 365 days (annual rotation).
     * Valid range: 90-3650 days.
     */
    @JsonProperty("rotationPeriodDays")
    @Schema(
            description = "Optional custom rotation period in days (default is 365 for annual rotation, min 90, max 3650)",
            example = "365",
            minimum = "90",
            maximum = "3650",
            nullable = true
    )
    private Integer rotationPeriodDays;

    /**
     * Reason for enabling/disabling rotation (for audit purposes).
     * Useful for tracking why rotation was changed.
     */
    @JsonProperty("reason")
    @Schema(
            description = "Optional reason for this rotation change (for audit trail)",
            example = "Compliance requirement for PCI-DSS",
            maxLength = 512,
            nullable = true
    )
    private String reason;

    /**
     * Whether this change should be applied immediately.
     * If false, may be scheduled for off-peak hours.
     */
    @JsonProperty("applyImmediately")
    @Schema(
            description = "Whether to apply this change immediately (true) or during maintenance window (false)",
            example = "false",
            defaultValue = "false"
    )
    private Boolean applyImmediately;

    /**
     * Creates a default request with annual rotation enabled.
     */
    public static UpdateKeyRotationRequestDto enableAnnualRotation() {
        return UpdateKeyRotationRequestDto.builder()
                .enableRotation(true)
                .rotationPeriodDays(365)
                .applyImmediately(true)
                .build();
    }

    /**
     * Creates a request to disable rotation.
     */
    public static UpdateKeyRotationRequestDto disableRotation() {
        return UpdateKeyRotationRequestDto.builder()
                .enableRotation(false)
                .applyImmediately(true)
                .build();
    }

    /**
     * Creates a request with custom rotation period.
     */
    public static UpdateKeyRotationRequestDto withCustomPeriod(Integer periodDays) {
        if (periodDays < 90 || periodDays > 3650) {
            throw new IllegalArgumentException(
                    "Rotation period must be between 90 and 3650 days. Got: " + periodDays
            );
        }
        return UpdateKeyRotationRequestDto.builder()
                .enableRotation(true)
                .rotationPeriodDays(periodDays)
                .applyImmediately(true)
                .build();
    }

    /**
     * Validates the request payload.
     *
     * @throws IllegalArgumentException if validation fails
     */
    public void validate() {
        if (enableRotation == null) {
            throw new IllegalArgumentException("enableRotation field is required");
        }

        if (enableRotation && rotationPeriodDays != null) {
            if (rotationPeriodDays < 90 || rotationPeriodDays > 3650) {
                throw new IllegalArgumentException(
                        "Rotation period must be between 90 and 3650 days. Got: " + rotationPeriodDays
                );
            }
        }

        if (reason != null && reason.length() > 512) {
            throw new IllegalArgumentException("Reason exceeds maximum length of 512 characters");
        }
    }

    /**
     * Gets the effective rotation period.
     * Returns the custom period if provided, otherwise the default (365 days).
     */
    public Integer getEffectiveRotationPeriod() {
        if (rotationPeriodDays != null) {
            return rotationPeriodDays;
        }
        return 365; // Default annual rotation
    }

    /**
     * Determines if this is a change from previous state.
     * Used to avoid redundant updates.
     *
     * @param currentlyEnabled whether rotation is currently enabled
     * @return true if this request changes the current state
     */
    public boolean isStateChange(boolean currentlyEnabled) {
        return !enableRotation.equals(currentlyEnabled);
    }
}
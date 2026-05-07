package eu.isygoit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for key usage statistics response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeyUsageStatsResponseDto {

    /**
     * The ID of the key
     */
    private Long keyId;

    /**
     * The ARN of the key
     */
    private String keyArn;

    /**
     * Total number of encrypt operations performed with this key
     */
    private Long encryptCount;

    /**
     * Total number of decrypt operations performed with this key
     */
    private Long decryptCount;

    /**
     * Total number of generate data key operations
     */
    private Long generateDataKeyCount;

    /**
     * Total number of re-encrypt operations
     */
    private Long reEncryptCount;

    /**
     * Date and time when the key was last used
     */
    private LocalDateTime lastUsedDate;

    /**
     * Date and time when the key was first used
     */
    private LocalDateTime firstUsedDate;

    /**
     * Average operations per day (last 30 days)
     */
    private Double averageOpsPerDay;

    /**
     * Usage by operation type breakdown
     * Map of operation type to count
     */
    private Map<String, Long> usageByOperation;

    /**
     * Hourly usage distribution (last 24 hours)
     * Map of hour to count
     */
    private Map<Integer, Long> hourlyDistribution;

    /**
     * Daily usage distribution (last 30 days)
     * Map of date to count
     */
    private Map<String, Long> dailyDistribution;

    /**
     * Whether usage tracking is enabled for this key
     */
    private Boolean usageTrackingEnabled;

    /**
     * The date when usage tracking started
     */
    private LocalDateTime trackingStartDate;
}
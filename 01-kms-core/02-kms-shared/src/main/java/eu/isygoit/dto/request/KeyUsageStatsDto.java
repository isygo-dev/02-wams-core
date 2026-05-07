package eu.isygoit.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeyUsageStatsDto {
    private Long keyId;
    private Long totalOperations;
    private Long encryptOperations;
    private Long decryptOperations;
    private Long signOperations;
    private Long verifyOperations;
    private Long failedOperations;
    private Double successRate;
    private Double averageLatencyMs;
    private LocalDateTime lastUsedAt;
    private Map<String, Long> operationsByType;
}
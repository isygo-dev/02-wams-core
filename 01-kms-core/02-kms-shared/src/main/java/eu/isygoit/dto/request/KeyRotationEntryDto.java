package eu.isygoit.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class KeyRotationEntryDto {

    private Long keyId;

    private String versionId;

    private LocalDateTime rotationDate;

    private String rotationType; // MANUAL or AUTOMATIC

    private String status;       // COMPLETED, FAILED, IN_PROGRESS

    private String performedBy;

    private String reason;       // Optional reason for rotation
}
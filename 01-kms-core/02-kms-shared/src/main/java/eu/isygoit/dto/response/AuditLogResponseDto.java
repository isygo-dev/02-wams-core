package eu.isygoit.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * The type Audit log response dto.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class AuditLogResponseDto {

    private List<AuditLogEntryDto> logs;

    /**
     * The type Audit log entry dto.
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @SuperBuilder
    public static class AuditLogEntryDto {
        private String action;
        private Long keyId;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime timestamp;

        private String principal;
        private String ip;
    }
}


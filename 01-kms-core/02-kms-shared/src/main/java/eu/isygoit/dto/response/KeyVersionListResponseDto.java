package eu.isygoit.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * The type Key version list response dto.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class KeyVersionListResponseDto {

    private List<KeyVersionDto> versions;

    /**
     * The type Key version dto.
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @SuperBuilder
    public static class KeyVersionDto {
        private String versionId;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime createdAt;

        private String status;
    }
}


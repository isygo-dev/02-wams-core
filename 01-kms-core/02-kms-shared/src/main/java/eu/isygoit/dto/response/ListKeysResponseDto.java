package eu.isygoit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * The type List keys response dto.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class ListKeysResponseDto {

    private List<KeySummaryDto> keys;

    private String nextToken;

    /**
     * The type Key summary dto.
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @SuperBuilder
    public static class KeySummaryDto {
        private String keyId;
        private String alias;
        private String status;
    }
}


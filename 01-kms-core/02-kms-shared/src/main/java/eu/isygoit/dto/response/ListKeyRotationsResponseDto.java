package eu.isygoit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for listing key rotations response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListKeyRotationsResponseDto {

    /**
     * List of key rotations
     */
    private List<RotationDto> rotations;

    /**
     * Token for pagination to get the next page of results
     */
    private String nextToken;

    /**
     * Total number of rotations
     */
    private Integer totalCount;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RotationDto {
        /**
         * The version ID of the rotated key
         */
        private String versionId;

        /**
         * The date and time when the rotation occurred
         */
        private LocalDateTime rotationDate;

        /**
         * The status of the rotated version
         */
        private String status;

        /**
         * Optional description of the rotation
         */
        private String description;
    }
}
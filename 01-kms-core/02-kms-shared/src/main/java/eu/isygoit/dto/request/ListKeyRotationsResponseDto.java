package eu.isygoit.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for listing key rotation history
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListKeyRotationsResponseDto {

    private List<KeyRotationEntryDto> rotations;

    private String nextToken;

    private Integer totalCount;
}
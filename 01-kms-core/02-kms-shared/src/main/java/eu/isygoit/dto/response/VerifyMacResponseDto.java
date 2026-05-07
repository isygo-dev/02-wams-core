package eu.isygoit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyMacResponseDto {
    private Long keyId;
    private Boolean macValid;
    private String macAlgorithm;
}
package eu.isygoit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportParametersResponseDto {
    private Long keyId;
    private String importToken;
    private String publicKey;
    private String wrappingAlgorithm;
    private String wrappingKeySpec;
    private LocalDateTime parametersValidTo;
}
package eu.isygoit.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicKeyResponseDto {
    private Long keyId;
    private String keySpec;
    private String keyUsage;
    private String signingAlgorithms;
    private String encryptionAlgorithms;
    private String publicKey;
}
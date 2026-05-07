package eu.isygoit.dto.response;

import eu.isygoit.enums.IEnumKeySpec;
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
    private IEnumKeySpec.Types keySpec;
    private String keyUsage;
    private String signingAlgorithms;
    private String encryptionAlgorithms;
    private String publicKey;
}
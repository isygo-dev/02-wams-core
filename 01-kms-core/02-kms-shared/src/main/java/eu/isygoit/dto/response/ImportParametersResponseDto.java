package eu.isygoit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for key material import parameters response
 * AWS KMS-compliant response for GetParametersForImport operation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportParametersResponseDto {

    /**
     * The ID of the key for which import parameters are being retrieved
     */
    private Long keyId;

    /**
     * The ARN of the key
     */
    private String keyArn;

    /**
     * The wrapping key (public key) used to encrypt the key material
     * This is typically an RSA public key
     */
    private byte[] wrappingKey;

    /**
     * The import token that must be used when importing the key material
     * This token binds the imported key material to the specific key
     */
    private byte[] importToken;

    /**
     * The algorithm used to encrypt the key material
     * Possible values: RSAES_PKCS1_V1_5, RSAES_OAEP_SHA_1, RSAES_OAEP_SHA_256
     */
    private String wrappingAlgorithm;

    /**
     * The validity period of the import parameters in hours
     * After this period, the parameters expire and cannot be used
     * Default is typically 24 hours
     */
    private Integer validityPeriodHours;

    /**
     * The expiration date and time of the import parameters
     */
    private LocalDateTime expirationDate;

    /**
     * The date and time when the parameters were generated
     */
    private LocalDateTime parametersGeneratedAt;
}
package eu.isygoit.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for key material import request
 * AWS KMS-compliant request for ImportKeyMaterial operation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportKeyMaterialRequestDto {

    /**
     * The encrypted key material
     * This should be encrypted using the wrapping key obtained from GetParametersForImport
     */
    @NotNull(message = "Encrypted key material is required")
    private byte[] encryptedKeyMaterial;

    /**
     * The import token obtained from GetParametersForImport
     */
    @NotNull(message = "Import token is required")
    private byte[] importToken;

    /**
     * The algorithm used to encrypt the key material
     * Must match the wrapping algorithm used when generating the parameters
     */
    private String wrappingAlgorithm;

    /**
     * The expiration date for the imported key material
     * After this date, the key material becomes invalid
     * Optional - if not set, the key material never expires
     */
    private LocalDateTime expirationDate;

    /**
     * Whether to expire the key material (if expirationDate is set)
     */
    private Boolean expireKeyMaterial;
}
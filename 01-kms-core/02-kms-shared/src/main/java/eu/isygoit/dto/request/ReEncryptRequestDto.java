package eu.isygoit.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for Re-Encrypt operation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReEncryptRequestDto {

    @NotBlank(message = "Source ciphertext is required")
    private String ciphertextBlob;

    private String sourceKeyId;

    @NotBlank(message = "Destination key ID is required")
    private Long destinationKeyId;

    private String sourceEncryptionContext;

    private String destinationEncryptionContext;

    // Optional: Specify algorithm if needed
    private String encryptionAlgorithm;
}
package eu.isygoit.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

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

    private Long sourceKeyId;

    @NotNull(message = "Destination key ID is required")
    private Long destinationKeyId;

    private Map<String, String> sourceEncryptionContext;

    private Map<String, String> destinationEncryptionContext;

    // Optional: Specify algorithm if needed
    private String encryptionAlgorithm;
}
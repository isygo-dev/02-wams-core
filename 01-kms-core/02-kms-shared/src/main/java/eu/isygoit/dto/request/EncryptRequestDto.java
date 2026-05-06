package eu.isygoit.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Map;

/**
 * The type Encrypt request dto.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class EncryptRequestDto {

    @NotBlank(message = "keyId cannot be blank")
    private String keyId;

    @NotBlank(message = "plaintext cannot be blank")
    private String plaintext;

    private Map<String, String> encryptionContext;
}


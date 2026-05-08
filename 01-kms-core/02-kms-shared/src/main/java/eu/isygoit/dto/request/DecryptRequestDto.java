package eu.isygoit.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Map;

/**
 * The type Decrypt request dto.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class DecryptRequestDto {

    private Long keyId;

    @NotBlank(message = "ciphertext cannot be blank")
    private String ciphertext;

    private Map<String, String> encryptionContext;
}


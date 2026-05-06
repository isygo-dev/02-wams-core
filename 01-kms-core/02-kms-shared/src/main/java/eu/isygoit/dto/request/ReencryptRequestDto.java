package eu.isygoit.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * The type Re-encrypt request dto.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class ReencryptRequestDto {

    @NotBlank(message = "ciphertext cannot be blank")
    private String ciphertext;

    @NotBlank(message = "destinationKeyId cannot be blank")
    private String destinationKeyId;
}


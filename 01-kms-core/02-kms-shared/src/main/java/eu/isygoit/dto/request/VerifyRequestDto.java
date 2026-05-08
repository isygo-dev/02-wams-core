package eu.isygoit.dto.request;

import eu.isygoit.enums.IEnumSigningAlgorithm;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * The type Verify request dto.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class VerifyRequestDto {

    @NotNull(message = "keyId cannot be blank")
    private Long keyId;

    @NotBlank(message = "message cannot be blank")
    private String message;

    @NotBlank(message = "signature cannot be blank")
    private String signature;

    @NotNull(message = "algorithm cannot be null")
    private IEnumSigningAlgorithm.Types algorithm;
}


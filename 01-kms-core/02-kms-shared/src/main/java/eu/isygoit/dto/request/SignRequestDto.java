package eu.isygoit.dto.request;

import eu.isygoit.enums.IEnumSigningAlgorithm;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * The type Sign request dto.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class SignRequestDto {

    @NotBlank(message = "keyId cannot be blank")
    private Long keyId;

    @NotBlank(message = "message cannot be blank")
    private String message;

    @NotNull(message = "algorithm cannot be null")
    private IEnumSigningAlgorithm.Types algorithm;
}


package eu.isygoit.dto.request;

import jakarta.validation.constraints.NotBlank;
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

    @NotBlank(message = "keyId cannot be blank")
    private String keyId;

    @NotBlank(message = "message cannot be blank")
    private String message;

    @NotBlank(message = "signature cannot be blank")
    private String signature;
}


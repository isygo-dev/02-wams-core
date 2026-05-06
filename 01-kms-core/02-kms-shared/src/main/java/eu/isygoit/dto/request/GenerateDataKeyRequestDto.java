package eu.isygoit.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * The type Generate data key request dto.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class GenerateDataKeyRequestDto {

    @NotBlank(message = "keyId cannot be blank")
    private String keyId;

    @Positive(message = "keySize must be positive")
    private Integer keySize;
}


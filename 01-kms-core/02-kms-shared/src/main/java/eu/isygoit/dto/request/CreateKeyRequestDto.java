package eu.isygoit.dto.request;

import eu.isygoit.enums.IEnumKeyPurpose;
import eu.isygoit.enums.IEnumKeySpec;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * The type Create key request dto.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class CreateKeyRequestDto {

    @NotNull(message = "keySpec cannot be null")
    private IEnumKeySpec.Types keySpec;

    @NotNull(message = "purpose cannot be null")
    private IEnumKeyPurpose.Types purpose;

    @Size(min = 1, max = 255, message = "alias must be between 1 and 255 characters")
    private String alias;

    @Size(max = 1024, message = "description must not exceed 1024 characters")
    private String description;
}


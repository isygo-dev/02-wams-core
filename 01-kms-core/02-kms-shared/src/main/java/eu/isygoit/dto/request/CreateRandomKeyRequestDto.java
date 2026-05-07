package eu.isygoit.dto.request;

import eu.isygoit.enums.IEnumCharSet;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRandomKeyRequestDto {
    @NotBlank
    private String keyName;

    @NotBlank
    private String tenant;

    @NotNull
    @Min(1)
    private Integer length;

    @NotNull
    private IEnumCharSet.Types charSetType;
}
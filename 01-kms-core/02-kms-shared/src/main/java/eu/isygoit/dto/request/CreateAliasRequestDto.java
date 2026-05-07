package eu.isygoit.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAliasRequestDto {
    @NotBlank
    private String aliasName;

    @NotBlank
    private Long targetKeyId;
}
package eu.isygoit.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportKeyMaterialRequestDto {
    @NotBlank
    private String encryptedKeyMaterial;

    @NotBlank
    private String importToken;

    private String wrappingAlgorithm;

    private LocalDateTime expirationModel;

    private Boolean enabled;
}
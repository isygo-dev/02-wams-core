package eu.isygoit.dto.request;

import eu.isygoit.enums.IEnumCustomKeyStoreType;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCustomKeyStoreRequestDto {
    @NotBlank
    private String keyStoreName;

    @NotBlank
    private IEnumCustomKeyStoreType.Types type;

    private String endpoint;

    private String username;

    private String password;

    private String certificate;

    private Map<String, String> configuration;
}
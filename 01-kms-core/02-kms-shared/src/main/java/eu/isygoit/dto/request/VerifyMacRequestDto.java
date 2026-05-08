package eu.isygoit.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyMacRequestDto {

    @NotNull
    private Long keyId;

    @NotBlank
    private String message;

    @NotBlank
    private String mac;

    @NotBlank
    private String macAlgorithm;

    private Map<String, String> encryptionContext;
}
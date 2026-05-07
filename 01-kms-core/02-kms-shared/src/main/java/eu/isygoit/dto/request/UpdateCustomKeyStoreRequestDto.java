package eu.isygoit.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCustomKeyStoreRequestDto {
    private String endpoint;
    private String username;
    private String password;
    private String certificate;
    private Boolean enabled;
    private String newKeyStoreName;
    private Map<String, String> configuration;
}
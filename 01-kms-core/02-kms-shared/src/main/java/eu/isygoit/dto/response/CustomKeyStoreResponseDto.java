package eu.isygoit.dto.response;

import eu.isygoit.enums.IEnumCustomKeyStoreStatus;
import eu.isygoit.enums.IEnumCustomKeyStoreType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomKeyStoreResponseDto {

    LocalDateTime lastSuccessfulConnection;
    private Long keyStoreId;
    private String keyStoreName;
    private IEnumCustomKeyStoreType.Types type;
    private IEnumCustomKeyStoreStatus.Types status;
    private String connectionState;
    private String endpoint;
    private String vendor;
    private String region;
    private Boolean connected;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String cloudHsmClusterId;
    private String xksProxyUriEndpoint;
    private String xksProxyUriPath;
    private Map<String, String> configuration;
}
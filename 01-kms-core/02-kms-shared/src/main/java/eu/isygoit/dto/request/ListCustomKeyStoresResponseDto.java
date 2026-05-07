package eu.isygoit.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListCustomKeyStoresResponseDto {
    private List<CustomKeyStoreResponseDto> customKeyStores;
    private String nextToken;
    private boolean truncated;
}
package eu.isygoit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListGrantsResponseDto {
    private List<GrantDto> grants;
    private String nextToken;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GrantDto {
        private String grantId;
        private String granteePrincipal;
        private String retiringPrincipal;
        private List<String> operations;
        private String constraints;
        private LocalDateTime createdAt;
    }
}
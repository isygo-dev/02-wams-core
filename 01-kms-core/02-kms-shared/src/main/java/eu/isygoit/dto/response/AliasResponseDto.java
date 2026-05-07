package eu.isygoit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AliasResponseDto {
    private String aliasName;
    private Long targetKeyId;
    private String targetKeyArn;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
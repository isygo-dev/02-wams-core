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
public class ReplicateKeyResponseDto {
    private String primaryKeyId;
    private String replicaKeyId;
    private String primaryRegion;
    private String replicaRegion;
    private String status;
    private LocalDateTime replicatedAt;
}
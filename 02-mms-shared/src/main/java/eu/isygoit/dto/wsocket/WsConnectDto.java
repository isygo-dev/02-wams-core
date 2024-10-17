package eu.isygoit.dto.wsocket;

import eu.isygoit.dto.extendable.AbstractAuditableDto;
import eu.isygoit.enums.IEnumWSStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * The type Ws connect dto.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class WsConnectDto extends AbstractAuditableDto<Long> {

    /**
     * The Status.
     */
    @Builder.Default
    IEnumWSStatus.Types status = IEnumWSStatus.Types.DISCONNECTED;
    private String sessionId;
    private Long senderId;
    private Long groupId;
}

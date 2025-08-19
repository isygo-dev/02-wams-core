package eu.isygoit.dto.wsocket;

import eu.isygoit.dto.IExchangeObjectDto;
import eu.isygoit.enums.IEnumWSBroker;
import eu.isygoit.enums.IEnumWSEndpoint;
import eu.isygoit.enums.IEnumWSStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * The type Ws subscribe dto.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder

public class WsSubscribeDto implements IExchangeObjectDto {

    /**
     * The Status.
     */
    @Builder.Default
    IEnumWSStatus.Types status = IEnumWSStatus.Types.DISCONNECTED;
    private String sessionId;
    private Long senderId;
    private Long groupId;
    private IEnumWSEndpoint.Types endPoint;
    private IEnumWSBroker.Types broker;
}

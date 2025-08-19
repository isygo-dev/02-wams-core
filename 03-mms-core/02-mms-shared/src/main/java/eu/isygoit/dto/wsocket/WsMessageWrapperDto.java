package eu.isygoit.dto.wsocket;


import eu.isygoit.dto.IExchangeObjectDto;
import eu.isygoit.enums.IEnumWSBroker;
import eu.isygoit.enums.IEnumWSEndpoint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * The type Ws message wrapper dto.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class WsMessageWrapperDto implements IExchangeObjectDto {

    private IEnumWSEndpoint.Types endPoint;
    private String freeEndoint; //used only if type is FREE
    private IEnumWSBroker.Types broker;
    private Long senderId; //userId
    private WsMessageDto message;
}

package eu.isygoit.dto.data;

import eu.isygoit.enums.IEnumWSStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Date;

/**
 * The type Chat account dto.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder

public class ChatAccountDto {

    private Long receiverId;
    private Long senderId;
    private String fromFullName;
    private String lastMessage;
    private Date date;
    private Boolean read = Boolean.FALSE;
    @Builder.Default
    private IEnumWSStatus.Types chatStatus = IEnumWSStatus.Types.DISCONNECTED;
}

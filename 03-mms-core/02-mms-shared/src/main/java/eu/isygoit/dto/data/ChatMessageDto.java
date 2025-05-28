package eu.isygoit.dto.data;

import eu.isygoit.dto.extendable.AbstractAuditableDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Date;
import java.util.UUID;

/**
 * The type Chat message dto.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder

public class ChatMessageDto extends AbstractAuditableDto<UUID> {

    private Long receiverId;
    private Long senderId;
    private String senderName;
    private String message;
    private Date date;
    private Boolean read = Boolean.FALSE;
}

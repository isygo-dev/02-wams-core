package eu.isygoit.model;

import eu.isygoit.model.jakarta.AbstractEntity;
import eu.isygoit.model.schema.SchemaColumnConstantName;
import eu.isygoit.model.schema.SchemaConstantSize;
import eu.isygoit.model.schema.SchemaTableConstantName;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.ColumnDefault;

import java.util.Date;

/**
 * The type Chat message.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = SchemaTableConstantName.T_CHAT_MESSAGE)
public class ChatMessage extends AbstractEntity<Long> {

    @Id
    @SequenceGenerator(name = "chat_message_sequence_generator", sequenceName = "chat_message_sequence", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "chat_message_sequence_generator")
    @Column(name = SchemaColumnConstantName.C_ID, updatable = false, nullable = false)
    private Long id;

    @Column(name = SchemaColumnConstantName.C_TO_ID, nullable = false)
    private Long receiverId;

    @Column(name = SchemaColumnConstantName.C_FROM_ID, nullable = false)
    private Long senderId;

    @Column(name = SchemaColumnConstantName.C_FROM_FULL_NAME, length = SchemaConstantSize.FROM_FULL_NAME)
    private String senderName;

    @Column(name = SchemaColumnConstantName.C_CHAT_MESSAGE, length = SchemaConstantSize.CHAT_MESSAGE, nullable = false)
    private String message;

    @OrderBy(SchemaColumnConstantName.C_MESSAGE_DATE + " DESC")
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = SchemaColumnConstantName.C_MESSAGE_DATE, nullable = false)
    private Date date;

    @Builder.Default
    @ColumnDefault("'false'")
    @Column(name = SchemaColumnConstantName.C_CHAT_MSG_READ, nullable = false)
    private Boolean read = Boolean.FALSE;

    /**
     * Gets group key.
     *
     * @return the group key
     */
    public String getGroupKey() {
        return Math.min(this.getReceiverId(), this.getSenderId()) + "-" + Math.max(this.getReceiverId(), this.getSenderId());
    }
}

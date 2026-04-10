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
import org.springframework.data.cassandra.core.mapping.CassandraType;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;

import java.util.Date;
import java.util.UUID;

/**
 * The type Chat message.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = SchemaTableConstantName.T_CHAT_MESSAGE)
public class ChatMessage extends AbstractEntity<UUID> {

    @PrimaryKey
    @CassandraType(type = CassandraType.Name.TIMEUUID)
    private UUID id;

    @CassandraType(type = CassandraType.Name.BIGINT)
    @Column(name = SchemaColumnConstantName.C_TO_ID, nullable = false)
    private Long receiverId;

    @CassandraType(type = CassandraType.Name.BIGINT)
    @Column(name = SchemaColumnConstantName.C_FROM_ID, nullable = false)
    private Long senderId;

    @CassandraType(type = CassandraType.Name.TEXT)
    @Column(name = SchemaColumnConstantName.C_FROM_FULL_NAME, length = SchemaConstantSize.FROM_FULL_NAME)
    private String senderName;

    @CassandraType(type = CassandraType.Name.TEXT)
    @Column(name = SchemaColumnConstantName.C_CHAT_MESSAGE, length = SchemaConstantSize.CHAT_MESSAGE, nullable = false)
    private String message;

    @CassandraType(type = CassandraType.Name.DATE)
    @OrderBy(SchemaColumnConstantName.C_MESSAGE_DATE + " DESC")
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = SchemaColumnConstantName.C_MESSAGE_DATE, nullable = false)
    private Date date;

    @CassandraType(type = CassandraType.Name.BOOLEAN)
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

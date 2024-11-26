package eu.isygoit.model;

import eu.isygoit.model.jakarta.AuditableEntity;
import eu.isygoit.model.schema.SchemaColumnConstantName;
import eu.isygoit.model.schema.SchemaTableConstantName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.cassandra.core.mapping.CassandraType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.util.List;
import java.util.UUID;

/**
 * The type Mail message.
 */
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Table(SchemaTableConstantName.T_MAIL_MESSAGE)
public class MailMessage extends AuditableEntity<UUID> implements ISAASEntity {

    @PrimaryKey
    @CassandraType(type = CassandraType.Name.TIMEUUID)
    private UUID id;

    @CassandraType(type = CassandraType.Name.TEXT)
    @Column(SchemaColumnConstantName.C_DOMAIN)
    private String domain;

    @CassandraType(type = CassandraType.Name.TEXT)
    @Column(SchemaColumnConstantName.C_SUBJECT)
    private String subject;

    @CassandraType(type = CassandraType.Name.TEXT)
    @Column(SchemaColumnConstantName.C_BODY)
    private String body;

    @CassandraType(type = CassandraType.Name.TEXT)
    @Column(SchemaColumnConstantName.C_TO_ADDRESS)
    private String toAddr;

    @CassandraType(type = CassandraType.Name.TEXT)
    @Column(SchemaColumnConstantName.C_CC_ADDRESS)
    private String ccAddr;

    @CassandraType(type = CassandraType.Name.TEXT)
    @Column(SchemaColumnConstantName.C_BCC_ADDRESS)
    private String bccAddr;

    @CassandraType(type = CassandraType.Name.LIST, typeArguments = CassandraType.Name.TEXT)
    @Column(SchemaColumnConstantName.C_ATTACHMENT)
    private List<String> attachments;

    @Builder.Default
    @CassandraType(type = CassandraType.Name.BOOLEAN)
    @Column(SchemaColumnConstantName.C_SENT)
    private Boolean sent = Boolean.FALSE;
}

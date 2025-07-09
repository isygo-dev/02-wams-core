package eu.isygoit.model;

import eu.isygoit.constants.TenantConstants;
import eu.isygoit.model.jakarta.AuditableEntity;
import eu.isygoit.model.schema.SchemaColumnConstantName;
import eu.isygoit.model.schema.SchemaConstantSize;
import eu.isygoit.model.schema.SchemaTableConstantName;
import eu.isygoit.model.schema.SchemaUcConstantName;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.ColumnDefault;

/**
 * The type Sender config.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = SchemaTableConstantName.T_MAIL_SENDER_CONFIG
        , uniqueConstraints = {
        @UniqueConstraint(name = SchemaUcConstantName.UC_SENDER_CONFIG_TENANT, columnNames = {SchemaColumnConstantName.C_TENANT})
})
public class SenderConfig extends AuditableEntity<Long> implements ITenantAssignable {

    @Id
    @SequenceGenerator(name = "sender_config_sequence_generator", sequenceName = "sender_config_sequence", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sender_config_sequence_generator")
    @Column(name = SchemaColumnConstantName.C_ID, updatable = false, nullable = false)
    private Long id;

    //@Convert(converter = LowerCaseConverter.class)
    @ColumnDefault("'" + TenantConstants.DEFAULT_TENANT_NAME + "'")
    @Column(name = SchemaColumnConstantName.C_TENANT, length = SchemaConstantSize.TENANT, updatable = false, nullable = false)
    private String tenant;
    @Column(name = SchemaColumnConstantName.C_HOST, nullable = false)
    private String host;
    @Column(name = SchemaColumnConstantName.C_PORT, nullable = false)
    private String port;
    @Column(name = SchemaColumnConstantName.C_USER_NAME, nullable = false)
    private String username;
    @Column(name = SchemaColumnConstantName.C_PASSWORD, nullable = false)
    private String password;
    @Column(name = SchemaColumnConstantName.C_TRANSPORT_PROTOCOL, nullable = false)
    private String transportProtocol;
    @Column(name = SchemaColumnConstantName.C_SMTP_AUTH)
    private String smtpAuth;
    @Builder.Default
    @ColumnDefault("'false'")
    @Column(name = SchemaColumnConstantName.C_SMTP_STARTTLS_ENABLE, nullable = false)
    private Boolean smtpStarttlsEnable = Boolean.FALSE;
    @Builder.Default
    @ColumnDefault("'false'")
    @Column(name = SchemaColumnConstantName.C_SMTP_STARTTLS_REQUIRED, nullable = false)
    private Boolean smtpStarttlsRequired = Boolean.FALSE;
    @Builder.Default
    @Column(name = SchemaColumnConstantName.C_DEBUG, nullable = false)
    private Boolean debug = Boolean.FALSE;
}

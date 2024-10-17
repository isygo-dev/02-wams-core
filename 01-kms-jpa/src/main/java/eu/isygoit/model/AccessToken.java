package eu.isygoit.model;

import eu.isygoit.enums.IEnumAppToken;
import eu.isygoit.model.jakarta.AuditableEntity;
import eu.isygoit.model.schema.ComSchemaConstantSize;
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
 * The type Access token.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Entity
@Table(name = SchemaTableConstantName.T_ACCESS_TOKEN)
public class AccessToken extends AuditableEntity<Long> {

    @Id
    @SequenceGenerator(name = "access_token_sequence_generator", sequenceName = "access_token_sequence", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "access_token_sequence_generator")
    @Column(name = SchemaColumnConstantName.C_ID, updatable = false, nullable = false)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = SchemaColumnConstantName.C_TOKEN_TYPE, length = IEnumAppToken.STR_ENUM_SIZE, nullable = false)
    private IEnumAppToken.Types tokenType;

    @Column(name = SchemaColumnConstantName.C_ACCOUNT_CODE, length = ComSchemaConstantSize.CODE, nullable = false)
    private String accountCode;

    @Column(name = SchemaColumnConstantName.C_APPLICATION, nullable = false)
    private String application;

    @Column(name = SchemaColumnConstantName.C_TOKEN, length = SchemaConstantSize.S_TOKEN, nullable = false)
    private String token;

    @Builder.Default
    @ColumnDefault("'false'")
    @Column(name = SchemaColumnConstantName.C_DEPRECATED, nullable = false)
    private Boolean deprecated = Boolean.FALSE;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = SchemaColumnConstantName.C_EXPIRY_DATE, nullable = false)
    private Date expiryDate;

    /**
     * Is expired boolean.
     *
     * @return the boolean
     */
    public boolean isExpired() {
        return this.expiryDate.before(new Date());
    }
}

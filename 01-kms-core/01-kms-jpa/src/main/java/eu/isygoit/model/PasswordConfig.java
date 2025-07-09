package eu.isygoit.model;

import eu.isygoit.constants.TenantConstants;
import eu.isygoit.enums.IEnumAuth;
import eu.isygoit.enums.IEnumCharSet;
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
 * The type Password config.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = SchemaTableConstantName.T_PASSWORD_CONFIG
        , uniqueConstraints = {
        @UniqueConstraint(name = SchemaUcConstantName.UC_PASSWORD_CONFIG_CODE,
                columnNames = {SchemaColumnConstantName.C_CODE}),
        @UniqueConstraint(name = SchemaUcConstantName.UC_PASSWORD_CONFIG_TENANT_TYPE,
                columnNames = {SchemaColumnConstantName.C_TENANT, SchemaColumnConstantName.C_TYPE})
})
public class PasswordConfig extends AuditableEntity<Long> implements ITenantAssignable, ICodeAssignable {

    @Id
    @SequenceGenerator(name = "password_config_sequence_generator", sequenceName = "password_config_sequence", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "password_config_sequence_generator")
    @Column(name = SchemaColumnConstantName.C_ID, updatable = false, nullable = false)
    private Long id;

    //@Convert(converter = LowerCaseConverter.class)
    @Column(name = SchemaColumnConstantName.C_CODE, length = SchemaConstantSize.CODE, updatable = false, nullable = false)
    private String code;
    //@Convert(converter = LowerCaseConverter.class)
    @ColumnDefault("'" + TenantConstants.DEFAULT_TENANT_NAME + "'")
    @Column(name = SchemaColumnConstantName.C_TENANT, length = SchemaConstantSize.TENANT, updatable = false, nullable = false)
    private String tenant;
    @ColumnDefault("'PWD'")
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = SchemaColumnConstantName.C_TYPE, length = IEnumAuth.STR_ENUM_SIZE, updatable = false, nullable = false)
    private IEnumAuth.Types type = IEnumAuth.Types.PWD;
    @Column(name = SchemaColumnConstantName.C_PATTERN)
    private String pattern;
    @Builder.Default
    @ColumnDefault("'ALL'")
    @Enumerated(EnumType.STRING)
    @Column(name = SchemaColumnConstantName.C_CHAR_SET_TYPE, length = IEnumCharSet.STR_ENUM_SIZE, nullable = false)
    private IEnumCharSet.Types charSetType = IEnumCharSet.Types.ALL;
    @Column(name = SchemaColumnConstantName.C_INITIAL, length = SchemaConstantSize.PASS_WORD)
    private String initial;
    @Builder.Default
    @Column(name = SchemaColumnConstantName.C_PWD_MIN_LENGTH, nullable = false)
    private Integer minLength = 8;
    @Builder.Default
    @Column(name = SchemaColumnConstantName.C_PWD_MAX_LENGTH, nullable = false)
    private Integer maxLength = 32;
    @Builder.Default
    @Column(name = SchemaColumnConstantName.C_LIFE_TIME, nullable = false)
    private Integer lifeTime = 90;
}

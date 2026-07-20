package eu.isygoit.model;

import eu.isygoit.constants.TenantConstants;
import eu.isygoit.enums.IEnumAccountOrigin;
import eu.isygoit.enums.IEnumRegistrationStatus;
import eu.isygoit.model.jakarta.AuditableEntity;
import eu.isygoit.model.schema.SchemaColumnConstantName;
import eu.isygoit.model.schema.SchemaConstantSize;
import eu.isygoit.model.schema.SchemaTableConstantName;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicUpdate;


/**
 * The type Registred user.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@DynamicUpdate
@Entity
@Table(name = SchemaTableConstantName.T_REGISTRED_USER, uniqueConstraints = {
        @UniqueConstraint(columnNames = {SchemaColumnConstantName.C_EMAIL}),
        @UniqueConstraint(columnNames = {SchemaColumnConstantName.C_TENANT})
})
public class RegisteredUser extends AuditableEntity<Long> implements ITenantAssignable {

    @Id
    @SequenceGenerator(name = "registred_user_sequence_generator", sequenceName = "registred_user_sequence", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "registred_user_sequence_generator")
    @Column(name = SchemaColumnConstantName.C_ID, updatable = false, nullable = false)
    private Long id;

    //@Convert(converter = LowerCaseConverter.class)
    @ColumnDefault("'" + TenantConstants.DEFAULT_TENANT_NAME + "'")
    @Column(name = SchemaColumnConstantName.C_TENANT, length = SchemaConstantSize.TENANT, updatable = false, nullable = false)
    private String tenant;

    //@Convert(converter = CamelCaseConverter.class)
    @Column(name = SchemaColumnConstantName.C_FIRST_NAME, length = SchemaConstantSize.S_NAME, nullable = false)
    private String firstName;

    //@Convert(converter = CamelCaseConverter.class)
    @Column(name = SchemaColumnConstantName.C_LAST_NAME, length = SchemaConstantSize.S_NAME, nullable = false)
    private String lastName;

    @Column(name = SchemaColumnConstantName.C_EMAIL, length = SchemaConstantSize.EMAIL, nullable = false, updatable = false)
    private String email;

    @Column(name = SchemaColumnConstantName.C_PHONE_NUMBER, length = SchemaConstantSize.PHONE_NUMBER, nullable = false)
    private String phoneNumber;

    @Column(name = SchemaColumnConstantName.C_ORGANISATION, length = SchemaConstantSize.S_NAME, nullable = false)
    private String organisation;

    @Column(name = SchemaColumnConstantName.C_FUNCTION_ROLE, length = SchemaConstantSize.S_NAME, nullable = false)
    private String functionRole;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'SIGNUP'")
    @Column(name = SchemaColumnConstantName.C_ORIGIN, length = IEnumAccountOrigin.STR_ENUM_SIZE, nullable = false)
    private IEnumAccountOrigin.Types origin = IEnumAccountOrigin.Types.SIGNUP;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'NEW'")
    @Column(name = SchemaColumnConstantName.C_STATUS, length = IEnumAccountOrigin.STR_ENUM_SIZE, nullable = false)
    private IEnumRegistrationStatus.Types status = IEnumRegistrationStatus.Types.NEW;
}

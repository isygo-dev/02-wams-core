package eu.isygoit.model;

import eu.isygoit.enums.IEnumAccountOrigin;
import eu.isygoit.model.jakarta.AuditableEntity;
import eu.isygoit.model.schema.SchemaColumnConstantName;
import eu.isygoit.model.schema.SchemaConstantSize;
import eu.isygoit.model.schema.SchemaTableConstantName;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;


/**
 * The type Registred user.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(name = SchemaTableConstantName.T_REGISTRED_USER, uniqueConstraints = {
        @UniqueConstraint(columnNames = {SchemaColumnConstantName.C_EMAIL})
})
public class RegistredUser extends AuditableEntity<Long> {

    @Id
    @SequenceGenerator(name = "registred_user_sequence_generator", sequenceName = "registred_user_sequence", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "registred_user_sequence_generator")
    @Column(name = SchemaColumnConstantName.C_ID, updatable = false, nullable = false)
    private Long id;

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

    @Builder.Default
    @Column(name = SchemaColumnConstantName.C_ORIGIN, length = IEnumAccountOrigin.STR_ENUM_SIZE, nullable = false)
    private String origin = IEnumAccountOrigin.Types.SYS_ADMIN.name();
}

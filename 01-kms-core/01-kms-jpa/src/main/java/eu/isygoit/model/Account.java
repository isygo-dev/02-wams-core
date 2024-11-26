package eu.isygoit.model;

import eu.isygoit.model.extendable.AccountModel;
import eu.isygoit.model.schema.*;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * The type Account.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = SchemaTableConstantName.T_ACCOUNT
        , uniqueConstraints = {
        @UniqueConstraint(name = SchemaUcConstantName.UC_ACCOUNT_CODE, columnNames = {SchemaColumnConstantName.C_CODE})
})
public class Account extends AccountModel<Long> {

    @Id
    @SequenceGenerator(name = "account_sequence_generator", sequenceName = "account_sequence", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "account_sequence_generator")
    @Column(name = SchemaColumnConstantName.C_ID, updatable = false, nullable = false)
    private Long id;

    @Column(name = SchemaColumnConstantName.C_FULL_NAME, length = ComSchemaConstantSize.S_NAME, nullable = false)
    private String fullName;

    @OrderBy(ComSchemaColumnConstantName.C_CREATE_DATE + " DESC")
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL /* Cascade only for OneToMany*/)
    @JoinColumn(name = SchemaColumnConstantName.C_USER_ID
            , foreignKey = @ForeignKey(name = SchemaFkConstantName.FK_PASSWORD_INFO_REF_ACCOUNT))
    private List<PasswordInfo> passwordInfos;
}

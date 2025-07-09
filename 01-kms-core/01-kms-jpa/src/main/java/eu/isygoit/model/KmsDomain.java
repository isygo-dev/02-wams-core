package eu.isygoit.model;

import eu.isygoit.model.tenancy.TenantModel;
import eu.isygoit.model.schema.SchemaColumnConstantName;
import eu.isygoit.model.schema.SchemaTableConstantName;
import eu.isygoit.model.schema.SchemaUcConstantName;
import eu.isygoit.model.tenancy.TenantModel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * The type Kms tenant.
 */
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = SchemaTableConstantName.T_TENANT
        , uniqueConstraints = {@UniqueConstraint(name = SchemaUcConstantName.UC_TENANT_NAME,
        columnNames = {SchemaColumnConstantName.C_NAME})
})
public class KmsDomain extends eu.isygoit.model.tenancy.TenantModel<Long> {

    @Id
    @SequenceGenerator(name = "tenant_sequence_generator", sequenceName = "tenant_sequence", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tenant_sequence_generator")
    @Column(name = SchemaColumnConstantName.C_ID, updatable = false, nullable = false)
    private Long id;

    /*    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL *//* CASCADE only for OneToOne*//*)
    @JoinColumn(name = SchemaColumnConstantName.C_PEB_CONFG, referencedColumnName = SchemaColumnConstantName.C_CODE
            , foreignKey = @ForeignKey(name = SchemaFkConstantName.FK_TENANT_REF_PEB_CONFIG))
    private PEBConfig pebConfig;*/

    /*    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL *//* CASCADE only for OneToOne*//*)
    @JoinColumn(name = SchemaColumnConstantName.C_DIGESTER_CONFIG, referencedColumnName = SchemaColumnConstantName.C_CODE
            , foreignKey = @ForeignKey(name = SchemaFkConstantName.FK_TENANT_REF_DIGESTER_CONFIG))
    private DigestConfig digestConfig;*/

    /*    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL *//* CASCADE only for OneToOne*//*)
    @JoinColumn(name = SchemaColumnConstantName.C_TOKEN_CONFIG, referencedColumnName = SchemaColumnConstantName.C_CODE
            , foreignKey = @ForeignKey(name = SchemaFkConstantName.FK_TENANT_REF_TOKEN_CONFIG))
    private TokenConfig tokenConfig;*/

    /*    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL *//* CASCADE only for OneToOne*//*)
    @JoinColumn(name = SchemaColumnConstantName.C_PASSWORD_CONFIG, referencedColumnName = SchemaColumnConstantName.C_CODE
            , foreignKey = @ForeignKey(name = SchemaFkConstantName.FK_TENANT_REF_PASSWORD_CONFIG))
    private PasswordConfig passwordConfig;*/

    /*    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL *//* Cascade only for OneToMany*//*)
    @JoinColumn(name = SchemaColumnConstantName.C_TENANT, referencedColumnName = SchemaColumnConstantName.C_NAME
            , foreignKey = @ForeignKey(name = SchemaFkConstantName.FK_ACCOUNT_REF_TENANT))
    private List<Account> accounts;*/
}

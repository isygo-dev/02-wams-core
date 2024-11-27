package eu.isygoit.model;

import eu.isygoit.model.extendable.CustomerModel;
import eu.isygoit.model.schema.*;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;


/**
 * The type Customer.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(name = SchemaTableConstantName.T_CUSTOMER, uniqueConstraints = {
        @UniqueConstraint(name = SchemaUcConstantName.UC_CUSTOMER, columnNames = {SchemaColumnConstantName.C_NAME})
})
public class Customer extends CustomerModel<Long> {

    @Id
    @SequenceGenerator(name = "customer_sequence_generator", sequenceName = "customer_sequence", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "customer_sequence_generator")
    @Column(name = SchemaColumnConstantName.C_ID, updatable = false, nullable = false)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL /* CASCADE only for OneToOne*/)
    @JoinColumn(name = SchemaColumnConstantName.C_ADDRESS, referencedColumnName = SchemaColumnConstantName.C_ID
            , foreignKey = @ForeignKey(name = SchemaFkConstantName.FK_CUSTOMER_REF_ADDRESS))
    private CustomerAddress address;

    @Column(name = SchemaColumnConstantName.C_ACCOUNT_CODE, length = SchemaConstantSize.CODE)
    private String accountCode;
}

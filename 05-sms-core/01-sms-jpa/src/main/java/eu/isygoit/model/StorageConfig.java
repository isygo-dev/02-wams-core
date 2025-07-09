package eu.isygoit.model;

import eu.isygoit.constants.TenantConstants;
import eu.isygoit.enums.IEnumStorage;
import eu.isygoit.model.jakarta.AuditableEntity;
import eu.isygoit.model.schema.SchemaColumnConstantName;
import eu.isygoit.model.schema.SchemaConstantSize;
import eu.isygoit.model.schema.SchemaTableConstantName;
import eu.isygoit.model.schema.SchemaUcConstantName;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.ColumnDefault;


/**
 * The type Storage config.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = SchemaTableConstantName.T_STORAGE_CONFIG
        , uniqueConstraints = {
        @UniqueConstraint(name = SchemaUcConstantName.UC_STORAGE_CONFIG_TENANT, columnNames = {SchemaColumnConstantName.C_TENANT})
})
public class StorageConfig extends AuditableEntity<Long> implements ITenantAssignable {

    @Id
    @SequenceGenerator(name = "storageConfig_sequence_generator", sequenceName = "storageConfig_sequence", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "storageConfig_sequence_generator")
    @Column(name = SchemaColumnConstantName.C_ID, updatable = false, nullable = false)
    private Long id;

    //@Convert(converter = LowerCaseConverter.class)
    @ColumnDefault("'" + TenantConstants.DEFAULT_TENANT_NAME + "'")
    @Column(name = SchemaColumnConstantName.C_TENANT, length = SchemaConstantSize.TENANT, updatable = false, nullable = false)
    private String tenant;

    @Enumerated(EnumType.STRING)
    @Column(name = SchemaColumnConstantName.C_TYPE, length = IEnumStorage.STR_ENUM_SIZE, nullable = false)
    private IEnumStorage.Types type;

    @Column(name = SchemaColumnConstantName.C_USER_NAME, nullable = false)
    private String userName;

    @Column(name = SchemaColumnConstantName.C_PASSWORD, nullable = false)
    private String password;

    @Column(name = SchemaColumnConstantName.C_URL, nullable = false)
    private String url;
}

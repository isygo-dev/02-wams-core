package eu.isygoit.model;

import eu.isygoit.model.extendable.ApiPermissionModel;
import eu.isygoit.model.schema.ComSchemaColumnConstantName;
import eu.isygoit.model.schema.SchemaColumnConstantName;
import eu.isygoit.model.schema.SchemaTableConstantName;
import eu.isygoit.model.schema.SchemaUcConstantName;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * The type Api permission.
 */
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = SchemaTableConstantName.T_API_PERMISSION, uniqueConstraints = {
        @UniqueConstraint(name = SchemaUcConstantName.UC_API_PERMISSION_KEY
                , columnNames = {SchemaColumnConstantName.C_SERVICE
                , SchemaColumnConstantName.C_OBJECT
                , SchemaColumnConstantName.C_METHOD
                , SchemaColumnConstantName.C_RQ_TYPE
                , SchemaColumnConstantName.C_PATH})
})
public class ApiPermission extends ApiPermissionModel<Long> {

    @Id
    @SequenceGenerator(name = "api_permission_sequence_generator", sequenceName = "api_permission_sequence", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "api_permission_sequence_generator")
    @Column(name = ComSchemaColumnConstantName.C_ID, updatable = false, nullable = false)
    private Long id;
}

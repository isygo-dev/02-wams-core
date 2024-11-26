package eu.isygoit.model;

import eu.isygoit.model.schema.ComSchemaColumnConstantName;
import eu.isygoit.model.schema.ComSchemaConstantSize;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

/**
 * The type Distinct api permission.
 */
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class DistinctApiPermission implements Serializable {

    @Column(name = ComSchemaColumnConstantName.C_SERVICE, length = ComSchemaConstantSize.S_NAME, nullable = false)
    private String serviceName;

    @Column(name = ComSchemaColumnConstantName.C_OBJECT, length = ComSchemaConstantSize.S_NAME, nullable = false)
    private String object;
}

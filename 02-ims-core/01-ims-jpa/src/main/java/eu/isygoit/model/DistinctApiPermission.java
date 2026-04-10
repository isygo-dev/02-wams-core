package eu.isygoit.model;

import eu.isygoit.model.schema.SchemaColumnConstantName;
import eu.isygoit.model.schema.SchemaConstantSize;
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

    @Column(name = SchemaColumnConstantName.C_SERVICE, length = SchemaConstantSize.S_NAME, nullable = false)
    private String serviceName;

    @Column(name = SchemaColumnConstantName.C_OBJECT, length = SchemaConstantSize.S_NAME, nullable = false)
    private String object;
}

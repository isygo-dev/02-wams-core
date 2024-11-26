package eu.isygoit.model;

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

/**
 * The type Category.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = SchemaTableConstantName.T_CATEGORY
        , uniqueConstraints = {
        @UniqueConstraint(
                name = SchemaUcConstantName.UC_CATEGORY,
                columnNames = {
                        SchemaColumnConstantName.C_NAME,
                })
})
public class Category extends AuditableEntity<Long> {

    @Id
    @SequenceGenerator(name = "category_sequence_generator", sequenceName = "category_sequence", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "category_sequence_generator")
    @Column(name = SchemaColumnConstantName.C_ID, updatable = false, nullable = false)
    private Long id;

    @Column(name = SchemaColumnConstantName.C_NAME, length = SchemaConstantSize.S_NAME, updatable = false, nullable = false)
    private String name;
    @Column(name = SchemaColumnConstantName.C_DESCRIPTION, length = SchemaConstantSize.DESCRIPTION)
    private String description;
}

package eu.isygoit.model;

import eu.isygoit.annotation.Criteria;
import eu.isygoit.constants.TenantConstants;
import eu.isygoit.model.extendable.LinkedFileModel;
import eu.isygoit.model.schema.*;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

/**
 * The type Linked file.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
@Entity
@Table(name = SchemaTableConstantName.T_LINKED_FILE
        , uniqueConstraints = {
        @UniqueConstraint(
                name = SchemaUcConstantName.UC_LINKED_FILE_CODE,
                columnNames = {
                        SchemaColumnConstantName.C_CODE
                }),
        @UniqueConstraint(
                name = SchemaUcConstantName.UC_LINKED_FILE,
                columnNames = {
                        SchemaColumnConstantName.C_TENANT,
                        SchemaColumnConstantName.C_CODE,
                        SchemaColumnConstantName.C_VERSION,
                })
})
public class LinkedFile extends LinkedFileModel<Long> implements ITenantAssignable {

    @Id
    @SequenceGenerator(name = "linkedfile_sequence_generator", sequenceName = "linkedfile_sequence", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "linkedfile_sequence_generator")
    @Column(name = SchemaColumnConstantName.C_ID, updatable = false, nullable = false)
    private Long id;

    //@Convert(converter = LowerCaseConverter.class)
    @ColumnDefault("'" + TenantConstants.DEFAULT_TENANT_NAME + "'")
    @Column(name = SchemaColumnConstantName.C_TENANT, length = SchemaConstantSize.TENANT, updatable = false, nullable = false)
    private String tenant;

    /**
     * Stored as a single JSON column (e.g. {"mimetype":"…","category":"…","extension":"…"}).
     * Keeps the existing C_TAG column — no join table required.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = ComSchemaColumnConstantName.C_TAG, columnDefinition = "jsonb")
    private Map<String, String> tags;

    @Criteria(1)
    @Column(name = ComSchemaColumnConstantName.C_CATEGORY, length = SchemaConstantSize.S_NAME)
    private String fileCategory;
}

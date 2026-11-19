package eu.isygoit.model;

import eu.isygoit.model.extendable.FileEntity;
import eu.isygoit.model.schema.ComSchemaColumnConstantName;
import eu.isygoit.model.schema.SchemaColumnConstantName;
import eu.isygoit.model.schema.SchemaTableConstantName;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

/**
 * The type Msg template file.
 */
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@DynamicUpdate
@Entity
@Table(name = SchemaTableConstantName.T_MSG_TEMPLATE_FILE)
public class MsgTemplateFile extends FileEntity<Long> implements IFileEntity {

    @Id
    @SequenceGenerator(name = "msg_template_file_sequence_generator", sequenceName = "msg_template_file_sequence", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "msg_template_file_sequence_generator")
    @Column(name = SchemaColumnConstantName.C_ID, updatable = false, nullable = false)
    private Long id;

    /**
     * Stored as a single JSON column (e.g. {"mimetype":"…","category":"…","extension":"…"}).
     * Keeps the existing C_TAG column — no join table required.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = ComSchemaColumnConstantName.C_TAG, columnDefinition = "jsonb")
    private Map<String, String> tags;
}

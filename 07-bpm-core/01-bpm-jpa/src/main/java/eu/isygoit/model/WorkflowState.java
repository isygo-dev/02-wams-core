package eu.isygoit.model;

import eu.isygoit.enums.IEnumPositionType;
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
 * The type Workflow state.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = SchemaTableConstantName.T_WORKFLOW_STATE, uniqueConstraints = {
        @UniqueConstraint(name = SchemaUcConstantName.UC_WORKFLOW_STATE_CODE,
                columnNames = {SchemaColumnConstantName.C_CODE})
})
public class WorkflowState extends AuditableEntity<Long> implements ICodifiable {

    @Id
    @SequenceGenerator(name = "workflow_state_sequence_generator", sequenceName = "workflow_state_sequence", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "workflow_state_sequence_generator")
    @Column(name = SchemaColumnConstantName.C_ID, updatable = false, nullable = false)
    private Long id;

    //@Convert(converter = LowerCaseConverter.class)
    @Column(name = SchemaColumnConstantName.C_CODE, length = SchemaConstantSize.CODE, updatable = false, nullable = false)
    private String code;
    @Column(name = SchemaColumnConstantName.C_NAME, length = SchemaConstantSize.S_NAME, updatable = false, nullable = false)
    private String name;
    @Column(name = SchemaColumnConstantName.C_DESCRIPTION, length = SchemaConstantSize.DESCRIPTION)
    private String description;
    //this column is uses to sort the states
    @Column(name = SchemaColumnConstantName.C_SEQUENCE)
    private Integer sequence;
    @Column(name = SchemaColumnConstantName.C_COLOR)
    private String color;
    @Enumerated(EnumType.STRING)
    @Column(name = SchemaColumnConstantName.C_POSITION_TYPE, length = IEnumPositionType.STR_ENUM_SIZE, nullable = false)
    private IEnumPositionType.Types positionType;

}

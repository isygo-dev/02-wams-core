package eu.isygoit.model;

import eu.isygoit.model.jakarta.AuditableEntity;
import eu.isygoit.model.schema.*;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.ColumnDefault;

import java.util.List;

/**
 * The type Workflow transition.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = SchemaTableConstantName.T_WORKFLOW_STATE_TRANSITION, uniqueConstraints = {
        @UniqueConstraint(name = SchemaUcConstantName.UC_WORKFLOW_TRANSITION,
                columnNames = {SchemaColumnConstantName.C_FROM_CODE, SchemaColumnConstantName.C_TO_CODE, SchemaColumnConstantName.C_WORKFLOW}),
        @UniqueConstraint(name = SchemaUcConstantName.UC_WORKFLOW_TRANSITION_CODE,
                columnNames = {SchemaColumnConstantName.C_CODE})
})
public class WorkflowTransition extends AuditableEntity<Long> implements ICodifiable {

    @Id
    @SequenceGenerator(name = "workflow_transition_sequence_generator", sequenceName = "workflow_transition_sequence", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "workflow_transition_sequence_generator")
    @Column(name = SchemaColumnConstantName.C_ID, updatable = false, nullable = false)
    private Long id;

    //@Convert(converter = LowerCaseConverter.class)
    @Column(name = SchemaColumnConstantName.C_CODE, length = SchemaConstantSize.CODE, updatable = false, nullable = false)
    private String code;
    @Column(name = SchemaColumnConstantName.C_FROM_CODE, length = SchemaConstantSize.S_NAME, updatable = false, nullable = false)
    private String fromCode;
    @Column(name = SchemaColumnConstantName.C_TO_CODE, length = SchemaConstantSize.S_NAME, updatable = false, nullable = false)
    private String toCode;

    @Column(name = SchemaColumnConstantName.C_WORKFLOW, updatable = false, insertable = false)
    private String workflow;

    @ElementCollection
    @CollectionTable(name = SchemaTableConstantName.T_WORKFLOW_TRANSITION_ITEM_TYPES,
            joinColumns = @JoinColumn(name = SchemaColumnConstantName.C_WORKFLOW_TRANSITION,
                    referencedColumnName = SchemaColumnConstantName.C_CODE,
                    foreignKey = @ForeignKey(name = SchemaFkConstantName.FK_ITEM_TYPE_REF_TRANSITION)))
    @Column(name = SchemaColumnConstantName.C_ITEM_TYPE)
    private List<String> itemTypes;

    @Column(name = SchemaColumnConstantName.C_TRANSITION_SERVICE, length = SchemaConstantSize.S_NAME, updatable = false, nullable = false)
    private String transitionService;
    @Builder.Default
    @Column(name = SchemaColumnConstantName.C_NOTIFY)
    private Boolean notify = Boolean.FALSE;
    @Builder.Default
    @ColumnDefault("'false'")
    @Column(name = SchemaColumnConstantName.C_BIDIRECTIONAL, nullable = false)
    private Boolean bidirectional = Boolean.FALSE;
    @ElementCollection
    @CollectionTable(name = SchemaTableConstantName.T_WORKFLOW_TRANSITION_WATCHERS
            , joinColumns = @JoinColumn(name = SchemaColumnConstantName.C_TRANSACTION,
            referencedColumnName = SchemaColumnConstantName.C_CODE,
            foreignKey = @ForeignKey(name = SchemaFkConstantName.FK_WATCHERS_REF_TRANSACTION)))
    @Column(name = SchemaColumnConstantName.C_EMAIL)
    private List<String> watchers;
}

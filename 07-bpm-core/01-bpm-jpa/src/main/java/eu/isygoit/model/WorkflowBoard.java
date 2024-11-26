package eu.isygoit.model;

import eu.isygoit.constants.DomainConstants;
import eu.isygoit.model.jakarta.AuditableEntity;
import eu.isygoit.model.schema.*;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.ColumnDefault;

import java.util.List;

/**
 * The type Workflow board.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = SchemaTableConstantName.T_WORKFLOW_BOARD, uniqueConstraints = {
        @UniqueConstraint(name = SchemaUcConstantName.UC_WORKFLOW_BOARD_CODE,
                columnNames = {SchemaColumnConstantName.C_CODE}),
        @UniqueConstraint(name = SchemaUcConstantName.UC_WORKFLOW_BOARD_DOMAIN_NAME,
                columnNames = {SchemaColumnConstantName.C_DOMAIN, SchemaColumnConstantName.C_NAME})
})
public class WorkflowBoard extends AuditableEntity<Long> implements ISAASEntity, ICodifiable {

    @Id
    @SequenceGenerator(name = "workflow_board_sequence_generator", sequenceName = "workflow_board_sequence", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "workflow_board_sequence_generator")
    @Column(name = SchemaColumnConstantName.C_ID, updatable = false, nullable = false)
    private Long id;

    //@Convert(converter = LowerCaseConverter.class)
    @ColumnDefault("'" + DomainConstants.DEFAULT_DOMAIN_NAME + "'")
    @Column(name = SchemaColumnConstantName.C_DOMAIN, length = SchemaConstantSize.DOMAIN, updatable = false, nullable = false)
    private String domain;

    //@Convert(converter = LowerCaseConverter.class)
    @Column(name = ComSchemaColumnConstantName.C_CODE, length = ComSchemaConstantSize.CODE, updatable = false, nullable = false)
    private String code;
    @Column(name = SchemaColumnConstantName.C_NAME, length = SchemaConstantSize.S_NAME, updatable = false, nullable = false)
    private String name;
    @Column(name = SchemaColumnConstantName.C_DESCRIPTION, length = ComSchemaConstantSize.DESCRIPTION)
    private String description;
    @Column(name = SchemaColumnConstantName.C_ITEM, nullable = false)
    private String item;
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL /* CASCADE only for OneToOne*/)
    @JoinColumn(name = SchemaColumnConstantName.C_WORKFLOW, referencedColumnName = SchemaColumnConstantName.C_CODE
            , foreignKey = @ForeignKey(name = SchemaFkConstantName.FK_BOARD_REF_WORKFLOW))
    private Workflow workflow;

    @ElementCollection
    @CollectionTable(name = SchemaTableConstantName.T_WORKFLOW_BOARD_WATCHERS
            , joinColumns = @JoinColumn(name = SchemaColumnConstantName.C_WORKFLOW_BOARD,
            referencedColumnName = SchemaColumnConstantName.C_CODE,
            foreignKey = @ForeignKey(name = SchemaFkConstantName.FK_WATCHERS_REF_WORKFLOW_BOARD)))
    @Column(name = SchemaColumnConstantName.C_EMAIL)
    private List<String> watchers;
}

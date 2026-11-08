package eu.isygoit.model;

import eu.isygoit.enums.IEnumLogLevel;
import eu.isygoit.model.jakarta.AbstractEntity;
import eu.isygoit.model.schema.SchemaColumnConstantName;
import eu.isygoit.model.schema.SchemaTableConstantName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(SchemaTableConstantName.T_PLAN_LOG_ENTRIES)
@ToString(callSuper = true)
public class PlanLog extends AbstractEntity<PlanLogKey> {
    @PrimaryKey
    private PlanLogKey id;

    @Column(SchemaColumnConstantName.C_LEVEL)
    private IEnumLogLevel.Types level;

    @Column(SchemaColumnConstantName.C_STEP)
    private String step;

    @Column(SchemaColumnConstantName.C_ENGINE)
    private String engine;

    @Column(SchemaColumnConstantName.C_MESSAGE)
    private String message;

    @Column(SchemaColumnConstantName.C_DETAILS)
    private String details;

    @Column(SchemaColumnConstantName.C_DURATION_MILLIS)
    private long durationMillis;

    @Column(SchemaColumnConstantName.C_TRACE_ID)
    private String traceId;

    @Column(SchemaColumnConstantName.C_SPAN_ID)
    private String spanId;

}

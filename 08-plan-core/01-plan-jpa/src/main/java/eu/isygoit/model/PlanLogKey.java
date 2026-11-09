package eu.isygoit.model;

import eu.isygoit.model.schema.SchemaColumnConstantName;
import eu.isygoit.model.schema.SchemaConstantSize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.cassandra.core.cql.Ordering;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@PrimaryKeyClass
public class PlanLogKey implements Serializable, ITenantAssignable {

    @PrimaryKeyColumn(
            name = SchemaColumnConstantName.C_JOB_ID,
            ordinal = 1,
            type = PrimaryKeyType.PARTITIONED
    )
    private Long jobId;

    @PrimaryKeyColumn(
            name = SchemaColumnConstantName.C_TENANT,
            ordinal = 0,
            type = PrimaryKeyType.PARTITIONED
    )
    private String tenant;

    @PrimaryKeyColumn(
            name = SchemaColumnConstantName.C_BUCKET_START,
            ordinal = 2,
            type = PrimaryKeyType.PARTITIONED
    )
    private Instant bucketStart;

    @PrimaryKeyColumn(
            name = SchemaColumnConstantName.C_EVENT_TIME,
            ordinal = 3,
            type = PrimaryKeyType.CLUSTERED,
            ordering = Ordering.DESCENDING
    )
    private Instant eventTime;

    @PrimaryKeyColumn(
            name = SchemaColumnConstantName.C_EVENT_ID,
            ordinal = 4,
            type = PrimaryKeyType.CLUSTERED,
            ordering = Ordering.DESCENDING
    )
    private UUID eventId;
}
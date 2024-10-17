package eu.isygoit.model;

import eu.isygoit.model.jakarta.AbstractEntity;
import eu.isygoit.model.schema.SchemaColumnConstantName;
import eu.isygoit.model.schema.SchemaConstantSize;
import eu.isygoit.model.schema.SchemaTableConstantName;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Date;


/**
 * The type Connection tracking.
 */
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = SchemaTableConstantName.T_CONNECTION_TRACKING)
public class ConnectionTracking extends AbstractEntity<Long> {

    @Id
    @SequenceGenerator(name = "connection_tracking_sequence_generator", sequenceName = "connection_tracking_sequence", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "connection_tracking_sequence_generator")
    @Column(name = SchemaColumnConstantName.C_ID, updatable = false, nullable = false)
    private Long id;

    @Column(name = SchemaColumnConstantName.C_BROWSER, length = SchemaConstantSize.BROWSER, nullable = false)
    private String browser;
    @Column(name = SchemaColumnConstantName.C_DEVICE, length = SchemaConstantSize.DEVICE, nullable = false)
    private String device;
    @Column(name = SchemaColumnConstantName.C_IP_ADDRESS, length = SchemaConstantSize.IP_ADDRESS, nullable = false)
    private String ipAddress;
    @Column(name = SchemaColumnConstantName.C_LOG_APP, length = SchemaConstantSize.S_NAME, nullable = false)
    private String logApp;
    @Temporal(value = TemporalType.TIMESTAMP)
    @Column(name = SchemaColumnConstantName.C_LOGIN_DATE, updatable = false)
    private Date loginDate;
}

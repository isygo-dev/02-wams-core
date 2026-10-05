package eu.isygoit.model;

import eu.isygoit.enums.QrLoginStatus;
import eu.isygoit.model.jakarta.AbstractEntity;
import eu.isygoit.model.schema.SchemaColumnConstantName;
import eu.isygoit.model.schema.SchemaConstantSize;
import eu.isygoit.model.schema.SchemaTableConstantName;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.DynamicUpdate;

import java.util.Date;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@DynamicUpdate
@Entity
@Table(name = SchemaTableConstantName.T_QR_LOGIN_CHALLENGE,
        uniqueConstraints = @UniqueConstraint(columnNames = SchemaColumnConstantName.C_QR_CHALLENGE_ID),
        indexes = @Index(name = "IDX_QR_CHALLENGE_EXPIRES", columnList = SchemaColumnConstantName.C_QR_EXPIRES_AT))
public class QrLoginChallenge extends AbstractEntity<Long> {

    @Id
    @SequenceGenerator(name = "qr_login_challenge_sequence_generator",
            sequenceName = "qr_login_challenge_sequence", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "qr_login_challenge_sequence_generator")
    @Column(name = SchemaColumnConstantName.C_ID, updatable = false, nullable = false)
    private Long id;

    @Column(name = SchemaColumnConstantName.C_QR_CHALLENGE_ID, length = 64, nullable = false, updatable = false)
    private String challengeId;

    @Column(name = SchemaColumnConstantName.C_QR_CHALLENGE_TENANT,
            length = SchemaConstantSize.TENANT, nullable = false, updatable = false)
    private String tenant;

    @Column(name = SchemaColumnConstantName.C_QR_CHALLENGE_USER,
            length = SchemaConstantSize.S_NAME, nullable = false, updatable = false)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(name = SchemaColumnConstantName.C_QR_STATUS, length = 16, nullable = false)
    private QrLoginStatus status;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = SchemaColumnConstantName.C_QR_EXPIRES_AT, nullable = false, updatable = false)
    private Date expiresAt;

    @Column(name = SchemaColumnConstantName.C_QR_APPROVED_BY, length = SchemaConstantSize.S_NAME)
    private String approvedBy;

    @Column(name = SchemaColumnConstantName.C_QR_APPROVED_TENANT, length = SchemaConstantSize.TENANT)
    private String approvedTenant;
}

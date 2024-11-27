package eu.isygoit.model;

import eu.isygoit.enums.IEnumAuth;
import eu.isygoit.enums.IEnumBinaryStatus;
import eu.isygoit.enums.IEnumPasswordStatus;
import eu.isygoit.model.jakarta.AuditableEntity;
import eu.isygoit.model.schema.SchemaColumnConstantName;
import eu.isygoit.model.schema.SchemaConstantSize;
import eu.isygoit.model.schema.SchemaTableConstantName;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.ColumnDefault;
import org.joda.time.LocalDateTime;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * The type Password info.
 */
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = SchemaTableConstantName.T_PASSWORD_INFO)
public class PasswordInfo extends AuditableEntity<Long> {

    @Id
    @SequenceGenerator(name = "password_info_sequence_generator", sequenceName = "password_info_sequence", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "password_info_sequence_generator")
    @Column(name = SchemaColumnConstantName.C_ID, updatable = false, nullable = false)
    private Long id;

    @Column(name = SchemaColumnConstantName.C_USER_ID, nullable = false, updatable = false)
    private Long userId;
    @Builder.Default
    @ColumnDefault("'PWD'")
    @Enumerated(EnumType.STRING)
    @Column(name = SchemaColumnConstantName.C_AUTH_TYPE, length = IEnumBinaryStatus.STR_ENUM_SIZE, nullable = false)
    private IEnumAuth.Types authType = IEnumAuth.Types.PWD;
    @Column(name = SchemaColumnConstantName.C_USER_PASSWORD, length = SchemaConstantSize.PASS_WORD, nullable = false, updatable = false)
    private String password;
    @Builder.Default
    @ColumnDefault("'DEPRECATED'")
    @Enumerated(EnumType.STRING)
    @Column(name = SchemaColumnConstantName.C_STATUS, length = IEnumPasswordStatus.STR_ENUM_SIZE, nullable = false)
    private IEnumPasswordStatus.Types status = IEnumPasswordStatus.Types.DEPRECATED;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = SchemaColumnConstantName.C_EXPIRY_DATE, nullable = false, updatable = false)
    private Date expiryDate;
    @Column(name = SchemaColumnConstantName.C_CRC16, nullable = false, updatable = false)
    private Integer crc16;
    @Column(name = SchemaColumnConstantName.C_CRC32, nullable = false, updatable = false)
    private Integer crc32;

    /**
     * Is expired boolean.
     *
     * @return the boolean
     */
    public boolean isExpired() {
        return !this.expiryDate.after(new Date());
    }

    /**
     * Remaining days long.
     *
     * @return the long
     */
    public Long remainingDays() {
        long diffInMillies = this.expiryDate.getTime() - (new Date()).getTime();
        if (diffInMillies <= 0) {
            return 0L;
        } else {
            return TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Will expire after boolean.
     *
     * @param days the days
     * @return the boolean
     */
    public boolean willExpireAfter(int days) {
        return LocalDateTime.fromDateFields(this.expiryDate).isBefore(LocalDateTime.now().plusDays(days));
    }
}

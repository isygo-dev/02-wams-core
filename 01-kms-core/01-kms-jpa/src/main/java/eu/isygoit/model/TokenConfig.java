package eu.isygoit.model;

import eu.isygoit.constants.TenantConstants;
import eu.isygoit.enums.IEnumCharSet;
import eu.isygoit.enums.IEnumToken;
import eu.isygoit.model.jakarta.AuditableEntity;
import eu.isygoit.model.schema.*;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicUpdate;

import java.util.Set;

/**
 * The type Token config.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
@Entity
@Table(name = SchemaTableConstantName.T_TOKEN_CONFIG
        , uniqueConstraints = {
        @UniqueConstraint(name = SchemaUcConstantName.UC_TOKEN_CONFIG_CODE,
                columnNames = {SchemaColumnConstantName.C_CODE}),
        @UniqueConstraint(name = SchemaUcConstantName.UC_TOKEN_CONFIG_TENANT_TYPE,
                columnNames = {SchemaColumnConstantName.C_TENANT, SchemaColumnConstantName.C_TOKEN_TYPE})
})
public class TokenConfig extends AuditableEntity<Long> implements ITenantAssignable, ICodeAssignable {

    @Id
    @SequenceGenerator(name = "token_config_sequence_generator", sequenceName = "token_config_sequence", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "token_config_sequence_generator")
    @Column(name = SchemaColumnConstantName.C_ID, updatable = false, nullable = false)
    private Long id;

    //@Convert(converter = LowerCaseConverter.class)
    @Column(name = SchemaColumnConstantName.C_CODE, length = SchemaConstantSize.CODE, updatable = false, nullable = false)
    private String code;

    //@Convert(converter = LowerCaseConverter.class)
    @ColumnDefault("'" + TenantConstants.DEFAULT_TENANT_NAME + "'")
    @Column(name = SchemaColumnConstantName.C_TENANT, length = SchemaConstantSize.TENANT, updatable = false, nullable = false)
    private String tenant;

    @Column(name = SchemaColumnConstantName.C_ISSUER, updatable = false, nullable = false)
    private String issuer;

    @Builder.Default
    @ColumnDefault("'ACCESS'")
    @Enumerated(EnumType.STRING)
    @Column(name = SchemaColumnConstantName.C_TOKEN_TYPE, length = IEnumToken.STR_ENUM_SIZE, nullable = false)
    private IEnumToken.Types tokenType = IEnumToken.Types.ACCESS;

    @ElementCollection
    @CollectionTable(name = SchemaTableConstantName.T_TOKEN_AUDIENCE
            , joinColumns = @JoinColumn(name = SchemaColumnConstantName.C_TOKEN_CONFIG,
            referencedColumnName = SchemaColumnConstantName.C_CODE,
            foreignKey = @ForeignKey(name = SchemaFkConstantName.FK_AUDIENCE_REF_TOKEN_CONFIG)))
    @Column(name = SchemaColumnConstantName.C_AUDIENCE)
    private Set<String> audience;

    @Builder.Default
    @ColumnDefault("'RS256'")
    @Column(name = SchemaColumnConstantName.C_SIGNATURE_ALGORITHM, length = IEnumCharSet.STR_ENUM_SIZE, nullable = false)
    private String signatureAlgorithm = "RS256";

    @Lob
    @Column(name = SchemaColumnConstantName.C_SECRET_KEY, nullable = false)
    private String secretKey = "sEcReTkEy";

    @Lob
    @Column(name = SchemaColumnConstantName.C_PUBLIC_KEY)
    private String publicKey = "pUbLiCkEy";

    @Builder.Default
    @ColumnDefault("14400000")
    @Column(name = SchemaColumnConstantName.C_LIFE_TIME_MS, nullable = false)
    private Integer lifeTimeInMs = 14400000;

    @Column(name = SchemaColumnConstantName.C_KMS_KEY_ID)
    private String kmsKeyId;

    /*
     For setting the used version of the KmsKey when a KMS key is linked to the token config. Not persisted, only for runtime use.
     */
    @Transient
    private String kmsKeyVersion;
}

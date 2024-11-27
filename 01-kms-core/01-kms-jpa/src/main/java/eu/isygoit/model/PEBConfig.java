package eu.isygoit.model;

import eu.isygoit.constants.DomainConstants;
import eu.isygoit.enums.IEnumAlgoPEBConfig;
import eu.isygoit.enums.IEnumIvGenerator;
import eu.isygoit.enums.IEnumSaltGenerator;
import eu.isygoit.enums.IEnumStringOutputType;
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
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.validator.constraints.Length;


/**
 * The type Peb config.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = SchemaTableConstantName.T_PEB_CONFIG
        , uniqueConstraints = {
        @UniqueConstraint(name = SchemaUcConstantName.UC_PEB_CONFIG_CODE,
                columnNames = {SchemaColumnConstantName.C_CODE}),
        @UniqueConstraint(name = SchemaUcConstantName.UC_PEB_CONFIG_DOMAIN,
                columnNames = {SchemaColumnConstantName.C_DOMAIN})
})
public class PEBConfig extends AuditableEntity<Long> implements ISAASEntity, ICodifiable {

    @Id
    @SequenceGenerator(name = "peb_config_sequence_generator", sequenceName = "peb_config_sequence", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "peb_config_sequence_generator")
    @Column(name = SchemaColumnConstantName.C_ID, updatable = false, nullable = false)
    private Long id;

    @Length(max = SchemaConstantSize.CODE)
    //@Convert(converter = LowerCaseConverter.class)
    @Column(name = SchemaColumnConstantName.C_CODE, length = SchemaConstantSize.CODE, updatable = false, nullable = false)
    private String code;

    //@Convert(converter = LowerCaseConverter.class)
    @ColumnDefault("'" + DomainConstants.DEFAULT_DOMAIN_NAME + "'")
    @Column(name = SchemaColumnConstantName.C_DOMAIN, length = SchemaConstantSize.DOMAIN, updatable = false, nullable = false)
    private String domain;

    //PBE ALGORITHMS:      [PBEWITHMD5ANDDES, PBEWITHMD5ANDTRIPLEDES, PBEWITHSHA1ANDDESEDE, PBEWITHSHA1ANDRC2_40]
    @Enumerated(EnumType.STRING)
    @Column(name = SchemaColumnConstantName.C_ALGORITHM, length = IEnumAlgoPEBConfig.STR_ENUM_SIZE, nullable = false)
    private IEnumAlgoPEBConfig.Types algorithm;

    @Column(name = SchemaColumnConstantName.C_PASSWORD)
    private String password;

    @Column(name = SchemaColumnConstantName.C_KEY_OBTENTION_ITERATIONS)
    private Integer keyObtentionIterations;

    @Enumerated(EnumType.STRING)
    @Column(name = SchemaColumnConstantName.C_SALT_GENERATOR, length = IEnumSaltGenerator.STR_ENUM_SIZE, nullable = false)
    private IEnumSaltGenerator.Types saltGenerator;

    @Enumerated(EnumType.STRING)
    @Column(name = SchemaColumnConstantName.C_IV_GENERATOR, length = IEnumIvGenerator.STR_ENUM_SIZE, nullable = false)
    private IEnumIvGenerator.Types ivGenerator;

    @Column(name = SchemaColumnConstantName.C_PROVIDER_CLASS_NAME)
    private String providerClassName;

    @Column(name = SchemaColumnConstantName.C_PROVIDER_NAME)
    private String providerName;

    @Column(name = SchemaColumnConstantName.C_POOL_SIZE)
    private Integer poolSize;

    @Enumerated(EnumType.STRING)
    @Column(name = SchemaColumnConstantName.C_STRING_OUTPUT_TYPE, length = IEnumStringOutputType.STR_ENUM_SIZE, nullable = false)
    private IEnumStringOutputType.Types stringOutputType;
}

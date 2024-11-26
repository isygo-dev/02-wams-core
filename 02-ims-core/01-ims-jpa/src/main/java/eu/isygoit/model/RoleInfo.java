package eu.isygoit.model;

import eu.isygoit.constants.DomainConstants;
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
 * The type Role info.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = SchemaTableConstantName.T_ROLE_INFO
        , uniqueConstraints = {
        @UniqueConstraint(name = SchemaUcConstantName.UC_ROLE_INFO_CODE,
                columnNames = {SchemaColumnConstantName.C_CODE}),
        @UniqueConstraint(name = SchemaUcConstantName.UC_ROLE_INFO_DOMAIN_NAME
                , columnNames = {SchemaColumnConstantName.C_DOMAIN, SchemaColumnConstantName.C_NAME})
})
public class RoleInfo extends AuditableEntity<Long> implements ISAASEntity, ICodifiable {

    @Id
    @SequenceGenerator(name = "role_info_sequence_generator", sequenceName = "role_info_sequence", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "role_info_sequence_generator")
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

    @Builder.Default
    @ColumnDefault("'0'")
    @Column(name = SchemaColumnConstantName.C_LEVEL, nullable = false)
    private Integer level = 0;

    @Column(name = SchemaColumnConstantName.C_DESCRIPTION, length = ComSchemaConstantSize.DESCRIPTION)
    private String description;

    @OrderBy(ComSchemaColumnConstantName.C_RANK + " ASC")
    @ManyToMany(fetch = FetchType.LAZY /* NO CASCADE */)
    @JoinTable(name = SchemaTableConstantName.T_ASSO_ROLE_INFO_APPLICATION,
            joinColumns = @JoinColumn(name = SchemaColumnConstantName.C_ROLE_INFO_CODE, referencedColumnName = SchemaColumnConstantName.C_CODE),
            inverseJoinColumns = @JoinColumn(name = SchemaColumnConstantName.C_APPLICATION_CODE, referencedColumnName = SchemaColumnConstantName.C_CODE))
    private List<Application> allowedTools;

    @ManyToMany(fetch = FetchType.LAZY /* NO CASCADE */)
    @JoinTable(name = SchemaTableConstantName.T_ASSO_ROLE_PERMISSION,
            joinColumns = @JoinColumn(name = SchemaColumnConstantName.C_ROLE, referencedColumnName = SchemaColumnConstantName.C_CODE),
            inverseJoinColumns = @JoinColumn(name = SchemaColumnConstantName.C_PERMISSION, referencedColumnName = SchemaColumnConstantName.C_ID))
    private List<ApiPermission> permissions;

    //Transient fields
    @Builder.Default
    @Transient
    private Integer numberOfUsers = 0;
}

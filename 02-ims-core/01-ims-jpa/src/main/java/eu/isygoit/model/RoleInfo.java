package eu.isygoit.model;

import eu.isygoit.constants.TenantConstants;
import eu.isygoit.model.jakarta.AuditableEntity;
import eu.isygoit.model.schema.SchemaColumnConstantName;
import eu.isygoit.model.schema.SchemaConstantSize;
import eu.isygoit.model.schema.SchemaTableConstantName;
import eu.isygoit.model.schema.SchemaUcConstantName;
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
        @UniqueConstraint(name = SchemaUcConstantName.UC_ROLE_INFO_TENANT_NAME
                , columnNames = {SchemaColumnConstantName.C_TENANT, SchemaColumnConstantName.C_NAME})
})
public class RoleInfo extends AuditableEntity<Long> implements ITenantAssignable, ICodeAssignable {

    @Id
    @SequenceGenerator(name = "role_info_sequence_generator", sequenceName = "role_info_sequence", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "role_info_sequence_generator")
    private Long id;

    //@Convert(converter = LowerCaseConverter.class)
    @ColumnDefault("'" + TenantConstants.DEFAULT_TENANT_NAME + "'")
    @Column(name = SchemaColumnConstantName.C_TENANT, length = SchemaConstantSize.TENANT, updatable = false, nullable = false)
    private String tenant;

    //@Convert(converter = LowerCaseConverter.class)
    @Column(name = SchemaColumnConstantName.C_CODE, length = SchemaConstantSize.CODE, updatable = false, nullable = false)
    private String code;

    @Column(name = SchemaColumnConstantName.C_NAME, length = SchemaConstantSize.S_NAME, updatable = false, nullable = false)
    private String name;

    @Builder.Default
    @ColumnDefault("'0'")
    @Column(name = SchemaColumnConstantName.C_LEVEL, nullable = false)
    private Integer level = 0;

    @Column(name = SchemaColumnConstantName.C_DESCRIPTION, length = SchemaConstantSize.DESCRIPTION)
    private String description;

    @OrderBy(SchemaColumnConstantName.C_RANK + " ASC")
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

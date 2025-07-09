package eu.isygoit.model;

import eu.isygoit.enums.*;
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

/**
 * The type Theme.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = SchemaTableConstantName.T_THEME, uniqueConstraints = {
        @UniqueConstraint(name = SchemaUcConstantName.UC_ACCOUNT_AND_TENANT, columnNames = {SchemaColumnConstantName.C_CODE_ACCOUNT, SchemaColumnConstantName.C_CODE_TENANT})
})
public class Theme extends AuditableEntity<Long> {

    @Id
    @SequenceGenerator(name = "theme_sequence_generator", sequenceName = "theme_sequence", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "theme_sequence_generator")
    @Column(name = SchemaColumnConstantName.C_ID, updatable = false, nullable = false)
    private Long id;

    @Column(name = SchemaColumnConstantName.C_CODE_TENANT, length = SchemaConstantSize.CODE, updatable = false, nullable = false)
    private String tenantCode;
    @Column(name = SchemaColumnConstantName.C_CODE_ACCOUNT, length = SchemaConstantSize.CODE, updatable = false, nullable = false)
    private String accountCode;
    @Column(name = SchemaColumnConstantName.C_COLOR)
    private String themeColor;
    @Builder.Default
    @ColumnDefault("'DEFAULT'")
    @Enumerated(EnumType.STRING)
    @Column(name = SchemaColumnConstantName.C_SKIN_TYPE, length = IEnumSkin.STR_ENUM_SIZE, nullable = false)
    private IEnumSkin.Types skin = IEnumSkin.Types.DEFAULT;
    @Builder.Default
    @ColumnDefault("'LIGHT'")
    @Enumerated(EnumType.STRING)
    @Column(name = SchemaColumnConstantName.C_MODE_TYPE, length = IEnumMode.STR_ENUM_SIZE, nullable = false)
    private IEnumMode.Types mode = IEnumMode.Types.LIGHT;
    @Builder.Default
    @ColumnDefault("'BOXED'")
    @Enumerated(EnumType.STRING)
    @Column(name = SchemaColumnConstantName.C_CONTENT_WIDTH, length = IEnumContentWidth.STR_ENUM_SIZE, nullable = false)
    private IEnumContentWidth.Types contentWidth = IEnumContentWidth.Types.BOXED;
    @Builder.Default
    @ColumnDefault("'STATIC'")
    @Enumerated(EnumType.STRING)
    @Column(name = SchemaColumnConstantName.C_BAR_TYPE, length = IEnumAppBarFooterType.STR_ENUM_SIZE, nullable = false)
    private IEnumAppBarFooterType.Types appBar = IEnumAppBarFooterType.Types.STATIC;

    @Builder.Default
    @ColumnDefault("'STATIC'")
    @Enumerated(EnumType.STRING)
    @Column(name = SchemaColumnConstantName.C_FOOTER_TYPE, length = IEnumAppBarFooterType.STR_ENUM_SIZE, nullable = false)
    private IEnumAppBarFooterType.Types footer = IEnumAppBarFooterType.Types.STATIC;
    @Builder.Default
    @ColumnDefault("'VERTICAL'")
    @Enumerated(EnumType.STRING)
    @Column(name = SchemaColumnConstantName.C_MENU_LAYOUT, length = IEnumMenuLayout.STR_ENUM_SIZE, nullable = false)
    private IEnumMenuLayout.Types layout = IEnumMenuLayout.Types.VERTICAL;
    @Builder.Default
    @ColumnDefault("'COLLAPSE'")
    @Enumerated(EnumType.STRING)
    @Column(name = SchemaColumnConstantName.C_MENU_TOGGLE, length = IEnumNavToggle.STR_ENUM_SIZE, nullable = false)
    private IEnumNavToggle.Types verticalNavToggleType = IEnumNavToggle.Types.COLLAPSE;

    @Builder.Default
    @ColumnDefault("'LTR'")
    @Enumerated(EnumType.STRING)
    @Column(name = SchemaColumnConstantName.C_MENU_DIRECTION, length = IEnumDirection.STR_ENUM_SIZE, nullable = false)
    private IEnumDirection.Types direction = IEnumDirection.Types.LTR;
    @Builder.Default
    @ColumnDefault("'false'")
    @Column(name = SchemaColumnConstantName.C_CHECK_BAR_BLUR, nullable = false)
    private Boolean appBarBlur = Boolean.FALSE;
    @Builder.Default
    @ColumnDefault("'true'")
    @Column(name = SchemaColumnConstantName.C_MENU_COLLAPSED, nullable = false)
    private Boolean navCollapsed = Boolean.TRUE;
}

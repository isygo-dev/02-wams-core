package eu.isygoit.model;


import eu.isygoit.constants.DomainConstants;
import eu.isygoit.enums.IEnumLanguage;
import eu.isygoit.enums.IEnumMsgTemplateName;
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
 * The type Msg template.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = SchemaTableConstantName.T_MSG_TEMPLATE,
        uniqueConstraints = {
                @UniqueConstraint(name = SchemaUcConstantName.UC_MSG_TEMPLATE_CODE,
                        columnNames = {SchemaColumnConstantName.C_CODE}),
                @UniqueConstraint(name = SchemaUcConstantName.UC_TEMPLATE_DOM_NAME_LANG,
                        columnNames = {SchemaColumnConstantName.C_DOMAIN, SchemaColumnConstantName.C_NAME, SchemaColumnConstantName.C_LANGUAGE})
        })
@SecondaryTable(name = SchemaTableConstantName.T_MSG_TEMPLATE_FILE,
        pkJoinColumns = @PrimaryKeyJoinColumn(name = SchemaColumnConstantName.C_ID,
                referencedColumnName = SchemaColumnConstantName.C_ID)
)
public class MsgTemplate extends AuditableEntity<Long>
        implements ISAASEntity, ICodifiable, IFileEntity {

    @Id
    @SequenceGenerator(name = "template_sequence_generator", sequenceName = "template_sequence", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "template_sequence_generator")
    @Column(name = SchemaColumnConstantName.C_ID, updatable = false, nullable = false)
    private Long id;

    //@Convert(converter = LowerCaseConverter.class)
    @Column(name = ComSchemaColumnConstantName.C_CODE, length = ComSchemaConstantSize.CODE, updatable = false, nullable = false)
    private String code;
    //@Convert(converter = LowerCaseConverter.class)
    @ColumnDefault("'" + DomainConstants.DEFAULT_DOMAIN_NAME + "'")
    @Column(name = SchemaColumnConstantName.C_DOMAIN, length = SchemaConstantSize.DOMAIN, updatable = false, nullable = false)
    private String domain;
    @Enumerated(EnumType.STRING)
    @Column(name = SchemaColumnConstantName.C_NAME, length = IEnumMsgTemplateName.STR_ENUM_SIZE, nullable = false)
    private IEnumMsgTemplateName.Types name;
    @Column(name = SchemaColumnConstantName.C_DESCRIPTION, length = ComSchemaConstantSize.DESCRIPTION)
    private String description;
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'EN'")
    @Column(name = SchemaColumnConstantName.C_LANGUAGE, length = IEnumLanguage.STR_ENUM_SIZE, nullable = false)
    private IEnumLanguage.Types language = IEnumLanguage.Types.EN;

    //BEGIN IFileEntity : SecondaryTable / MsgTemplateFile
    @Column(name = SchemaColumnConstantName.C_FILE_NAME, table = SchemaTableConstantName.T_MSG_TEMPLATE_FILE)
    private String fileName;
    @Column(name = SchemaColumnConstantName.C_ORIGINAL_FILE_NAME, table = SchemaTableConstantName.T_MSG_TEMPLATE_FILE)
    private String originalFileName;
    @ColumnDefault("'NA'")
    @Column(name = SchemaColumnConstantName.C_PATH, table = SchemaTableConstantName.T_MSG_TEMPLATE_FILE)
    private String path;
    @Column(name = SchemaColumnConstantName.C_EXTENSION, table = SchemaTableConstantName.T_MSG_TEMPLATE_FILE)
    private String extension;
    @Column(name = SchemaColumnConstantName.C_TYPE, table = SchemaTableConstantName.T_MSG_TEMPLATE_FILE)
    private String type;

    @ElementCollection
    @CollectionTable(name = SchemaTableConstantName.T_MSG_TEMPLATE_FILE_TAGS
            , joinColumns = @JoinColumn(name = SchemaColumnConstantName.C_MSG_TEMPLATE,
            referencedColumnName = SchemaColumnConstantName.C_ID,
            foreignKey = @ForeignKey(name = SchemaFkConstantName.FK_TAGS_REF_MSG_TEMPLATE_FILE)))
    @Column(name = SchemaColumnConstantName.C_TAG_OWNER)
    private List<String> tags;
    //END IFileEntity : SecondaryTable
}

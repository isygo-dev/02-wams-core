package eu.isygoit.model;

import eu.isygoit.constants.DomainConstants;
import eu.isygoit.model.extendable.LinkedFileModel;
import eu.isygoit.model.schema.*;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.ColumnDefault;

import java.util.List;

/**
 * The type Linked file.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = SchemaTableConstantName.T_LINKED_FILE
        , uniqueConstraints = {
        @UniqueConstraint(
                name = SchemaUcConstantName.UC_LINKED_FILE_CODE,
                columnNames = {
                        SchemaColumnConstantName.C_CODE
                }),
        @UniqueConstraint(
                name = SchemaUcConstantName.UC_LINKED_FILE,
                columnNames = {
                        SchemaColumnConstantName.C_DOMAIN,
                        SchemaColumnConstantName.C_ORIGINAL_FILE_NAME,
                        SchemaColumnConstantName.C_VERSION,
                })
})
public class LinkedFile extends LinkedFileModel<Long> implements IDomainAssignable {

    @Id
    @SequenceGenerator(name = "linkedfile_sequence_generator", sequenceName = "linkedfile_sequence", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "linkedfile_sequence_generator")
    @Column(name = SchemaColumnConstantName.C_ID, updatable = false, nullable = false)
    private Long id;

    //@Convert(converter = LowerCaseConverter.class)
    @ColumnDefault("'" + DomainConstants.DEFAULT_DOMAIN_NAME + "'")
    @Column(name = SchemaColumnConstantName.C_DOMAIN, length = SchemaConstantSize.DOMAIN, updatable = false, nullable = false)
    private String domain;

    @ElementCollection
    @CollectionTable(name = SchemaTableConstantName.T_LINKED_FILE_TAGS
            , joinColumns = @JoinColumn(name = SchemaColumnConstantName.C_LINKED_FILE,
            referencedColumnName = SchemaColumnConstantName.C_CODE,
            foreignKey = @ForeignKey(name = SchemaFkConstantName.FK_TAGS_REF_LINKED_FILE)))
    @Column(name = SchemaColumnConstantName.C_TAG_OWNER)
    private List<String> tags;

    @ManyToMany(fetch = FetchType.LAZY /* NO CASCADE */)
    @JoinTable(name = SchemaTableConstantName.T_ASSO_LINKED_FILE_CATEGORY, joinColumns = {
            @JoinColumn(foreignKey = @ForeignKey(name = SchemaFkConstantName.FK_CATEGORY_REF_LINKED_FILE),
                    name = SchemaColumnConstantName.C_FILE,
                    referencedColumnName = SchemaColumnConstantName.C_CODE, nullable = false, updatable = false)}
            , inverseJoinColumns = {
            @JoinColumn(foreignKey = @ForeignKey(name = SchemaFkConstantName.FK_LINKED_FILE_REF_CATEGORY),
                    name = SchemaColumnConstantName.C_CATEGORY,
                    referencedColumnName = SchemaColumnConstantName.C_NAME, nullable = false, updatable = false)}
            , uniqueConstraints = {@UniqueConstraint(name = SchemaUcConstantName.UC_FILE_CATEGORY, columnNames = {SchemaColumnConstantName.C_FILE, SchemaColumnConstantName.C_CATEGORY})})
    private List<Category> categories;
}

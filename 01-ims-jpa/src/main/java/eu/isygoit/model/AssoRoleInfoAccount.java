package eu.isygoit.model;

import eu.isygoit.model.schema.SchemaColumnConstantName;
import eu.isygoit.model.schema.SchemaConstantSize;
import eu.isygoit.model.schema.SchemaTableConstantName;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;


@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = SchemaTableConstantName.T_ASSO_ROLE_INFO_ACCOUNT)
public class AssoRoleInfoAccount implements Serializable {

    @EmbeddedId
    private AssoRoleInfoAccountId id;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Embeddable
    public class AssoRoleInfoAccountId implements Serializable {

        @Column(name = SchemaColumnConstantName.C_ROLE_INFO_CODE, length = SchemaConstantSize.CODE, nullable = false)
        private String roleInfoCode;

        @Column(name = SchemaColumnConstantName.C_ACCOUNT_CODE, length = SchemaConstantSize.CODE, nullable = false)
        private String accountCode;
    }
}
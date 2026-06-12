package eu.isygoit.dto.data;

import eu.isygoit.dto.extendable.AuditableIdAssignableDto;
import eu.isygoit.enums.IEnumAuth;
import eu.isygoit.enums.IEnumCharSet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;


/**
 * The type Password config dto.
 */
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class PasswordConfigDto extends AuditableIdAssignableDto<Long> {

    private Long id;
    private String code;
    private String tenant;
    private IEnumAuth.Types type;
    private String pattern;
    private IEnumCharSet.Types charSetType;
    private String initial;
    private Integer minLength;
    private Integer maxLength;
    private Integer lifeTime;
}

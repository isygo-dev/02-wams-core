package eu.isygoit.dto.data;

import eu.isygoit.dto.extendable.AuditableDto;
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
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class PasswordConfigDto extends AuditableDto<Long> {

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

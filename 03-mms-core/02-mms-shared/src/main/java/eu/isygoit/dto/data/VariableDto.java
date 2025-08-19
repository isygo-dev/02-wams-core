package eu.isygoit.dto.data;


import eu.isygoit.dto.extendable.AbstractAuditableDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * The type Variable dto.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class VariableDto extends AbstractAuditableDto<Long> {

    private Long id;
    private String key;
    private String value;
}

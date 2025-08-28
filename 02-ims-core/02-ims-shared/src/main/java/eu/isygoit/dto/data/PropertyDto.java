package eu.isygoit.dto.data;

import eu.isygoit.dto.extendable.AuditableDto;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;


/**
 * The type Property dto.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class PropertyDto extends AuditableDto<Long> {

    private Long id;
    private String guiName;
    @NotEmpty
    private String name;

    private String value;

}


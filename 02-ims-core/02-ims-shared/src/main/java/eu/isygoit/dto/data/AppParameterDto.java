package eu.isygoit.dto.data;


import eu.isygoit.dto.extendable.AuditableDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * The type App parameter dto.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class AppParameterDto extends AuditableDto<Long> {

    private Long id;
    private String name;
    private String value;
    private String tenant;
    private String description;
}

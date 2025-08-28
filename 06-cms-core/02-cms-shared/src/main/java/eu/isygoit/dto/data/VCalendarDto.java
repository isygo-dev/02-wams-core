package eu.isygoit.dto.data;


import eu.isygoit.dto.extendable.AuditableDto;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * The type V calendar dto.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class VCalendarDto extends AuditableDto<Long> {

    private Long id;
    @NotEmpty
    private String tenant;
    private String code;
    @NotEmpty
    private String name;
    private String icsPath;
    @Builder.Default
    private Boolean locked = Boolean.FALSE;
    private String description;
}

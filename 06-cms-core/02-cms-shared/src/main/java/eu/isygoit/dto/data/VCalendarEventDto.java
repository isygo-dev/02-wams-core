package eu.isygoit.dto.data;

import eu.isygoit.dto.extendable.AbstractAuditableDto;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Date;

/**
 * The type V calendar event dto.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class VCalendarEventDto extends AbstractAuditableDto<Long> {

    @NotNull
    @NotEmpty
    private String tenant;
    private String code;
    @NotNull
    @NotEmpty
    private String calendar;
    @NotNull
    @NotEmpty
    private String name;
    private String title;
    @NotNull
    @NotEmpty
    private String type;
    @NotNull
    private Date startDate;
    @NotNull
    private Date endDate;
}

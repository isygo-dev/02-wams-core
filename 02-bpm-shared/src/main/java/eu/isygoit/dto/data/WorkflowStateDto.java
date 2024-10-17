package eu.isygoit.dto.data;

import eu.isygoit.dto.extendable.AbstractAuditableDto;
import eu.isygoit.enums.IEnumPositionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * The type Workflow state dto.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class WorkflowStateDto extends AbstractAuditableDto<Long> {

    private String name;
    private String code;
    private String wbCode;
    private String description;
    private Integer sequence;
    private String color;
    private IEnumPositionType.Types positionType;
}

package eu.isygoit.dto.data;

import eu.isygoit.dto.extendable.AbstractAuditableDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * The type Workflow board dto.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class WorkflowBoardDto extends AbstractAuditableDto<Long> {

    private String domain;
    private String code;
    private String name;
    private String description;
    private String item;
    private WorkflowDto workflow;
    private List<String> watchers;
}

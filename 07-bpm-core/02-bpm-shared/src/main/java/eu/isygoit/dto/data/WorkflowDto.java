package eu.isygoit.dto.data;

import eu.isygoit.dto.extendable.AbstractAuditableDto;
import eu.isygoit.enums.IEnumWorkflow;
import eu.isygoit.enums.IEnumWorkflowCategory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * The type Workflow dto.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class WorkflowDto extends AbstractAuditableDto<Long> {

    private String tenant;
    private String code;
    private String name;
    private String description;
    private IEnumWorkflowCategory.Types category;
    private IEnumWorkflow.Types type;
    private List<WorkflowStateDto> workflowStates;
    private List<WorkflowTransitionDto> workflowTransitions;
}

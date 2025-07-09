package eu.isygoit.repository;

import eu.isygoit.model.WorkflowBoard;
import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAndCodeAssignableRepository;

/**
 * The interface Workflow board repository.
 */
public interface WorkflowBoardRepository extends JpaPagingAndSortingTenantAndCodeAssignableRepository<WorkflowBoard, Long> {

}

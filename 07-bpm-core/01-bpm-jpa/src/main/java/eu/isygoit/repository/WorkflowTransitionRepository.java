package eu.isygoit.repository;

import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAndCodeAssignableRepository;
import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAndCodeAssignableRepository;
import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAssignableRepository;

import eu.isygoit.model.WorkflowTransition;

/**
 * The interface Workflow transition repository.
 */
public interface WorkflowTransitionRepository extends JpaPagingAndSortingCodeAssingnableRepository<WorkflowTransition, Long> {
}

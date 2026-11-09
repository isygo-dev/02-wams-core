package eu.isygoit.model.schema;

/**
 * The interface Schema index constant name.
 * Declares all @Index constant names of the plan module (plain indexes only: Hibernate generates the schema and
 * cannot express partial indexes).
 */
public interface SchemaIndexConstantName {

    String IDX_PLAN_JOB_STATUS_SUBMITTED = "IDX_PLAN_JOB_STATUS_SUBMITTED";
    String IDX_PLAN_JOB_TENANT_SUBMITTED = "IDX_PLAN_JOB_TENANT_SUBMITTED";
    String IDX_PLAN_JOB_STATUS_DISPATCHED_SUBMITTED = "IDX_PLAN_JOB_STATUS_DISPATCHED_SUBMITTED";
}

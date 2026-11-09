package eu.isygoit.solver.spi;

/**
 * What a {@link ConstraintFamily} expects its {@code Constraint.scope} to select. Used by the validation stage to check
 * {@code Constraint.scope.kind} against the family (see ENGINE_V2_DESIGN 4.3, "Scope semantics per family").
 * <p>
 * Pure enum: immutable, thread-safe.
 */
public enum ScopeKind {
    /** Scope selects tasks (kind = all | task | taskType | tag). */
    TASKS,
    /** Scope selects resources (kind = all | resource | resourceId | resourceType | tag of a resource). */
    RESOURCES,
    /** Scope is a task-tag group: kind = tag (one group) or kind = all together with a groupByTagPrefix param. */
    GROUPS,
    /** The family ignores the scope entirely (for example forbidden_pair). */
    NONE
}

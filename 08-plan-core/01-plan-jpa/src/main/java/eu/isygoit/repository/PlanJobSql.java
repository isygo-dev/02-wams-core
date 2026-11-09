package eu.isygoit.repository;

import eu.isygoit.model.schema.SchemaColumnConstantName;

/**
 * Shared native SQL fragments of {@link PlanJobRepository}.
 */
final class PlanJobSql {
    /**
     * Audit actor written by the native statements (workers and API transitions alike).
     */
    static final String SYSTEM_ACTOR = "plan-service";

    /**
     * Trailing part of every status-transition UPDATE "set" clause: audit columns + optimistic version.
     */
    static final String SET_AUDIT_AND_VERSION =
            SchemaColumnConstantName.C_UPDATE_DATE + " = localtimestamp, "
                    + SchemaColumnConstantName.C_UPDATED_BY + " = '" + SYSTEM_ACTOR + "', "
                    + SchemaColumnConstantName.C_VERSION + " = " + SchemaColumnConstantName.C_VERSION + " + 1 ";

    /**
     * Message column is plain text bounded to 1000 characters.
     */
    static final String MESSAGE_PARAM = "left(cast(:message as text), 1000)";

    static final String ERROR_CODE_PARAM = "left(cast(:errorCode as text), 128)";

    private PlanJobSql() {
    }
}

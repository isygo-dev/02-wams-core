package eu.isygoit.model.schema;

/**
 * The interface Schema fk constant name.
 */
public interface SchemaFkConstantName extends ComSchemaFkConstantName {

    /**
     * The constant FK_WORKFLOW_REF_STATE.
     */
    String FK_WORKFLOW_REF_STATE = "FK_WORKFLOW_REF_START_STATE";
    /**
     * The constant FK_WORKFLOW_REF_TRANSITION.
     */
    String FK_WORKFLOW_REF_TRANSITION = "FK_WORKFLOW_REF_TRANSITION";
    /**
     * The constant FK_BOARD_REF_WORKFLOW.
     */
    String FK_BOARD_REF_WORKFLOW = "FK_BOARD_REF_WORKFLOW";
    /**
     * The constant FK_ITEM_TYPE_REF_TRANSITION.
     */
    String FK_ITEM_TYPE_REF_TRANSITION = "FK_ITEM_TYPE_REF_TRANSITION";
    /**
     * The constant FK_WATCHERS_REF_WORKFLOW_BOARD.
     */
    String FK_WATCHERS_REF_WORKFLOW_BOARD = "FK_WATCHERS_REF_WORKFLOW_BOARD";
    /**
     * The constant FK_WATCHERS_REF_TRANSACTION.
     */
    String FK_WATCHERS_REF_TRANSACTION = "FK_WATCHERS_REF_TRANSACTION";
}

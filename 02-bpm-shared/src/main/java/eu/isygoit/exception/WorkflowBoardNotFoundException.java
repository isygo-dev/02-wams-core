package eu.isygoit.exception;


import eu.isygoit.annotation.MsgLocale;


/**
 * The type Workflow board not found exception.
 */
@MsgLocale("workflow.board.not.found.exception")
public class WorkflowBoardNotFoundException extends ManagedException {

    /**
     * Instantiates a new Workflow board not found exception.
     *
     * @param s the s
     */
    public WorkflowBoardNotFoundException(String s) {
        super(s);
    }
}

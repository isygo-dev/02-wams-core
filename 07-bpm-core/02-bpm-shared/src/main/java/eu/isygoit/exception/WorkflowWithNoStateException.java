package eu.isygoit.exception;


import eu.isygoit.annotation.MsgLocale;


/**
 * The type Workflow with no state exception.
 */
@MsgLocale("workflow.with.no.state.exception")
public class WorkflowWithNoStateException extends ManagedException {

    /**
     * Instantiates a new Workflow with no state exception.
     *
     * @param s the s
     */
    public WorkflowWithNoStateException(String s) {
        super(s);
    }
}

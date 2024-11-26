package eu.isygoit.exception;


import eu.isygoit.annotation.MsgLocale;


/**
 * The type Workflow not parametrize exception.
 */
@MsgLocale("workflow.not.parametrized.exception")
public class WorkflowNotParametrizeException extends ManagedException {

    /**
     * Instantiates a new Workflow not parametrize exception.
     *
     * @param s the s
     */
    public WorkflowNotParametrizeException(String s) {
        super(s);
    }
}

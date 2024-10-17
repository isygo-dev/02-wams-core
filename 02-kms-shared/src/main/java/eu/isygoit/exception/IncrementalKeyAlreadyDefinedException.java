package eu.isygoit.exception;


import eu.isygoit.annotation.MsgLocale;


/**
 * The type Incremental key already defined exception.
 */
@MsgLocale("incremental.key.already.defined.exception")
public class IncrementalKeyAlreadyDefinedException extends ManagedException {

    /**
     * Instantiates a new Incremental key already defined exception.
     *
     * @param s the s
     */
    public IncrementalKeyAlreadyDefinedException(String s) {
        super(s);
    }
}

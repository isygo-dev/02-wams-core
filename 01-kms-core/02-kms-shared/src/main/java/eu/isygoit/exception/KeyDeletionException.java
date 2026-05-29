package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("schedule.key.deletion.exception")
public class KeyDeletionException extends ManagedException {

    /**
     * Instantiates a new Token config not found exception.
     *
     * @param s the s
     */
    public KeyDeletionException(String s) {
        super(s);
    }

    public KeyDeletionException(String s, Throwable cause) {
        super(s, cause);
    }
}

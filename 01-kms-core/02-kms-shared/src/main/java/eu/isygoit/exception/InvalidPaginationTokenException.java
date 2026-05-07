package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("invalidpaginationtokenexception.exception")
public class InvalidPaginationTokenException extends ManagedException {

    /**
     * Instantiates a new Token config not found exception.
     *
     * @param s the s
     */
    public InvalidPaginationTokenException(String s) {
        super(s);
    }
}

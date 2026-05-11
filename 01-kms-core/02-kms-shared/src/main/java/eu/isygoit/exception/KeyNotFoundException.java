package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

/**
 * The type Key not found exception.
 */
@MsgLocale("key.not.found.exception")
public class KeyNotFoundException extends RuntimeException {

    public KeyNotFoundException(String s) {
        super(s);
    }
}


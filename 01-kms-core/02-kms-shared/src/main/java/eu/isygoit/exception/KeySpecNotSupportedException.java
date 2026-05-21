package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("key.spec.not.supported.exception")
public class KeySpecNotSupportedException extends RuntimeException {

    public KeySpecNotSupportedException(String s) {
        super(s);
    }

    public KeySpecNotSupportedException(String s, Throwable cause) {
        super(s, cause);
    }
}
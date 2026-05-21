package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("key.spec.not.yet.supported.exception")
public class KeySpecNotYetSupportedException extends RuntimeException {

    public KeySpecNotYetSupportedException(String s) {
        super(s);
    }
}
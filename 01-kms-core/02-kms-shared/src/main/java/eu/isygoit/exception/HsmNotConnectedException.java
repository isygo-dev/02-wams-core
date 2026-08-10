package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("hsm.not.connected.exception")
public class HsmNotConnectedException extends ManagedException {

    public HsmNotConnectedException(String message) {
        super(message);
    }

    public HsmNotConnectedException(String message, Throwable cause) {
        super(message, cause);
    }
}

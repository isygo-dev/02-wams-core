package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

/**
 * The type Grant not found exception.
 */
@MsgLocale("grant.not.foundexception")
public class GrantNotFoundException extends RuntimeException {

    /**
     * Instantiates a new Grant not found exception.
     *
     * @param grantId the grant id
     */
    public GrantNotFoundException(String grantId) {
        super("Grant not found: " + grantId);
    }

    /**
     * Instantiates a new Grant not found exception.
     *
     * @param message the message
     * @param cause   the cause
     */
    public GrantNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}


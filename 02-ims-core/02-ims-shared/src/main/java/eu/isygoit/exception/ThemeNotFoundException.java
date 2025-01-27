package eu.isygoit.exception;


import eu.isygoit.annotation.MsgLocale;


/**
 * The type Account authentication exception.
 */
@MsgLocale("theme.not.found.exception")
public class ThemeNotFoundException extends ManagedException {

    /**
     * Instantiates a new Account authentication exception.
     *
     * @param s the s
     */
    public ThemeNotFoundException(String s) {
        super(s);
    }
}

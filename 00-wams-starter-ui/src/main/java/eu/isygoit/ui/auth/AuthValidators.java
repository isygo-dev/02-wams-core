package eu.isygoit.ui.auth;

import com.vaadin.flow.data.validator.EmailValidator;
import com.vaadin.flow.data.validator.StringLengthValidator;

public final class AuthValidators {

    private AuthValidators() {
    }

    public static StringLengthValidator length(String message, int minimum, int maximum) {
        return new StringLengthValidator(message, minimum, maximum);
    }

    public static EmailValidator email(String message) {
        return new EmailValidator(message);
    }
}

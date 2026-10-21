package eu.isygoit.ui.common.validation;

import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.validator.EmailValidator;
import com.vaadin.flow.data.validator.RegexpValidator;
import eu.isygoit.ui.common.component.PhoneField;

import java.util.function.Supplier;

/**
 * Shared field validators for email, phone and date-period coherence, reused across
 * all dialogs/panels in starter-ui and the application modules (pilot, kms, ims, mms,
 * dms, sms, cms) instead of ad-hoc per-dialog checks.
 */
public final class CommonValidators {

    private CommonValidators() {
    }

    public static EmailValidator email(String message) {
        return new EmailValidator(message);
    }

    public static RegexpValidator phone(String message) {
        return new RegexpValidator(message, PhoneField.PATTERN, true);
    }

    /**
     * Validates that a date/time field's value is not strictly before a reference value
     * read at validation time (e.g. an end date vs. a start date). Null values on either
     * side are treated as "not yet known" and pass, since {@code required} fields already
     * enforce presence separately.
     */
    public static <T extends Comparable<? super T>> Validator<T> notBefore(Supplier<T> referenceSupplier, String message) {
        return (value, ctx) -> {
            T reference = referenceSupplier.get();
            if (value == null || reference == null || value.compareTo(reference) >= 0) {
                return ValidationResult.ok();
            }
            return ValidationResult.error(message);
        };
    }

    /**
     * Validates that a date/time field's value is strictly after a reference value
     * read at validation time (e.g. an end date that must not equal the start date).
     */
    public static <T extends Comparable<? super T>> Validator<T> after(Supplier<T> referenceSupplier, String message) {
        return (value, ctx) -> {
            T reference = referenceSupplier.get();
            if (value == null || reference == null || value.compareTo(reference) > 0) {
                return ValidationResult.ok();
            }
            return ValidationResult.error(message);
        };
    }
}

package eu.isygoit.ui.common.component;

import com.vaadin.flow.component.textfield.Autocomplete;
import com.vaadin.flow.component.textfield.TextField;

/**
 * A phone number {@link TextField} with an input mask (client-side allowed-character
 * filter) and a loose, international (E.164-like) format: an optional leading {@code +}
 * followed by 7 to 18 digits, with spaces, dots, dashes or parentheses allowed as visual
 * separators (e.g. {@code +33 6 12 34 56 78}).
 * <p>
 * Server-side format validation is a separate concern: bind this field with
 * {@link eu.isygoit.ui.common.validation.CommonValidators#phone(String)}.
 * <p>
 * The value is always left-to-right (e.g. {@code +33 6 12 34 56 78}), the same in every
 * locale including Arabic: no locale-specific grouping/inversion is ever applied, and the
 * field forces an LTR direction via {@link BidiLtr#apply} so the browser's bidi algorithm
 * can't visually reorder it in a right-to-left page.
 */
public class PhoneField extends TextField {

    /** Characters a user is allowed to type at all (client-side input mask). */
    public static final String ALLOWED_CHAR_PATTERN = "[0-9+ .\\-()]";

    /** Full-value format enforced server-side via {@code CommonValidators.phone(...)}. */
    public static final String PATTERN = "^\\+?[0-9][0-9 .\\-()]{6,17}[0-9]$";

    public static final String DEFAULT_PLACEHOLDER = "+33 6 12 34 56 78";

    /**
     * Wraps a phone number for safe read-only display next to right-to-left text (cards,
     * grids, details dialogs): the digits/{@code +} always render in the same left-to-right
     * order, in every locale including Arabic.
     */
    public static String forDisplay(String phone) {
        return BidiLtr.wrap(phone);
    }

    public PhoneField() {
        this(null);
    }

    public PhoneField(String label) {
        if (label != null) {
            setLabel(label);
        }
        setAllowedCharPattern(ALLOWED_CHAR_PATTERN);
        setPattern(PATTERN);
        setPlaceholder(DEFAULT_PLACEHOLDER);
        setClearButtonVisible(true);
        setMaxLength(20);
        setAutocomplete(Autocomplete.TEL);
        getElement().setAttribute("inputmode", "tel");
        BidiLtr.apply(getElement());
    }
}

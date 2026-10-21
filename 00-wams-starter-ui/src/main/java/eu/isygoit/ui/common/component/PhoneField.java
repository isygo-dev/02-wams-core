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
 * field forces {@code dir="ltr"} so the browser's bidi algorithm can't visually reorder it
 * in a right-to-left page.
 */
public class PhoneField extends TextField {

    /** Characters a user is allowed to type at all (client-side input mask). */
    public static final String ALLOWED_CHAR_PATTERN = "[0-9+ .\\-()]";

    /** Full-value format enforced server-side via {@code CommonValidators.phone(...)}. */
    public static final String PATTERN = "^\\+?[0-9][0-9 .\\-()]{6,17}[0-9]$";

    public static final String DEFAULT_PLACEHOLDER = "+33 6 12 34 56 78";

    /** Unicode "left-to-right isolate" / "pop directional isolate": forces a value to
     * render left-to-right as a self-contained run, wherever it ends up (a grid cell, a
     * card row, a tooltip, a chip) — unlike the {@code dir="ltr"} attribute on the input
     * field itself, this also covers every READ-ONLY, plain-text display of the same
     * value, which otherwise gets visually reordered by the browser's bidi algorithm when
     * shown inside Arabic (right-to-left) text. */
    private static final String LRI = "⁦";
    private static final String PDI = "⁩";

    /**
     * Wraps a phone number for safe read-only display next to right-to-left text (cards,
     * grids, details dialogs): the digits/{@code +} always render in the same left-to-right
     * order, in every locale including Arabic.
     */
    public static String forDisplay(String phone) {
        return (phone == null || phone.isBlank()) ? phone : LRI + phone + PDI;
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
        getElement().setAttribute("dir", "ltr");
    }
}

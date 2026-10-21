package eu.isygoit.ui.common.component;

import com.vaadin.flow.component.datepicker.DatePicker;

/**
 * A {@link DatePicker} configured for locale-aware entry: {@code dd/MM/yyyy} for every
 * locale except Arabic, which uses {@code yyyy/MM/dd} (see {@link LocaleAwareDateFormat}).
 * The user can type the date freely (including lenient variants such as single-digit
 * day/month, {@code -} separators, or ISO {@code yyyy-MM-dd}) as well as pick it from the
 * calendar overlay; the locale's own pattern is always used to display and re-parse the
 * value, so it is never ambiguous with the US {@code MM/dd/yyyy} order.
 * <p>
 * The typed/displayed value (just digits and separators, no letters) is forced left-to-right
 * via {@link BidiLtr#apply}, the same fix as {@link PhoneField}: without it, the Unicode Bidi
 * Algorithm can reorder the whole value inside a right-to-left (Arabic) page since nothing in
 * it is a strong-direction character.
 */
public class MaskedDatePicker extends DatePicker {

    public MaskedDatePicker() {
        this(null);
    }

    public MaskedDatePicker(String label) {
        if (label != null) {
            setLabel(label);
        }
        applyMask(this);
    }

    /**
     * Applies the same locale-aware mask/parsing to a plain {@link DatePicker} that was
     * already created elsewhere (e.g. a generated form), without needing to replace its
     * type with {@link MaskedDatePicker}.
     */
    public static void applyMask(DatePicker datePicker) {
        String displayFormat = LocaleAwareDateFormat.pattern();
        DatePicker.DatePickerI18n i18n = new DatePicker.DatePickerI18n();
        i18n.setDateFormats(displayFormat,
                "d/M/yyyy", "dd-MM-yyyy", "d-M-yyyy", "yyyy-MM-dd", "yyyy/MM/dd", "yyyy/M/d");
        datePicker.setI18n(i18n);
        datePicker.setPlaceholder(displayFormat.toLowerCase());
        BidiLtr.apply(datePicker.getElement());
    }
}

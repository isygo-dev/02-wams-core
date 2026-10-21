package eu.isygoit.ui.common.component;

import com.vaadin.flow.component.datepicker.DatePicker;

/**
 * Links a start/end {@link DatePicker} pair so the calendar overlay itself refuses an
 * incoherent period: picking a start date raises the end picker's minimum, and picking
 * an end date caps the start picker's maximum. This is a UI-level guard only; still bind
 * both fields with {@link eu.isygoit.ui.common.validation.CommonValidators#notBefore}
 * (or {@code after}) so the coherence rule also holds for values entered by typing and
 * for server-side validation.
 */
public final class DatePeriodBinder {

    private DatePeriodBinder() {
    }

    public static void bind(DatePicker start, DatePicker end) {
        start.addValueChangeListener(e -> end.setMin(e.getValue()));
        end.addValueChangeListener(e -> start.setMax(e.getValue()));
    }
}

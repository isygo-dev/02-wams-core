package eu.isygoit.ui.common.filters;

import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import eu.isygoit.i18n.I18n;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Compact date‑range filter used in two ways:
 *
 * <ul>
 *   <li><b>Metadata mode</b> — a {@code fieldName} is provided; the filter
 *       can produce a criteria clause consumable by
 *       {@link eu.isygoit.helper.CriteriaHelper}.</li>
 *   <li><b>Manual mode</b> — {@code fieldName} is {@code null}; the filter
 *       notifies an external listener via {@link #setOnChange(Runnable)}.</li>
 * </ul>
 */
public class DateRangeFilter extends HorizontalLayout {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final String fieldName;
    private final DatePicker from = new DatePicker();
    private final DatePicker to   = new DatePicker();

    private Runnable onChange;

    public DateRangeFilter(String fieldName, String label) {
        this(fieldName,
                label,
                label + " " + I18n.t("common.filter.date.from"),
                label + " " + I18n.t("common.filter.date.to"));
    }

    public DateRangeFilter(String fieldName, String label,
                           String fromLabel, String toLabel) {
        this.fieldName = fieldName;
        if (label == null) throw new IllegalArgumentException("label must not be null");

        setSpacing(false);
        setPadding(false);
        setAlignItems(Alignment.END);
        addClassName("filter-date-range");          // ← renamed from "date-range-filter"

        from.setLabel(fromLabel);
        from.setClearButtonVisible(true);
        from.addClassName("filter-date");           // ← added
        from.addValueChangeListener(e -> fireOnChange());

        to.setLabel(toLabel);
        to.setClearButtonVisible(true);
        to.addClassName("filter-date");             // ← added
        to.addValueChangeListener(e -> fireOnChange());

        add(from, to);
    }

    public void setOnChange(Runnable onChange) { this.onChange = onChange; }

    public LocalDate getFromValue() { return from.getValue(); }
    public LocalDate getToValue()   { return to.getValue(); }

    public boolean isEmpty() { return from.isEmpty() && to.isEmpty(); }

    public void clear() {
        from.clear();
        to.clear();
    }

    public String toCriteria() {
        if (fieldName == null) return null;

        LocalDate f = from.getValue();
        LocalDate t = to.getValue();

        if (f == null && t == null) return null;

        if (f != null && t != null) {
            return fieldName + " BW ('" + f.format(FMT) + "', '" + t.format(FMT) + "')";
        }
        if (f != null) return fieldName + " >= '" + f.format(FMT) + "'";
        return fieldName + " <= '" + t.format(FMT) + "'";
    }

    private void fireOnChange() {
        if (onChange != null) onChange.run();
    }
}
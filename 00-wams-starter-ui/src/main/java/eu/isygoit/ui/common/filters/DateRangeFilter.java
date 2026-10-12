package eu.isygoit.ui.common.filters;

import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import eu.isygoit.i18n.I18n;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Compact date-range filter used in two ways:
 *
 * <ul>
 *   <li><b>Metadata mode</b> — a {@code fieldName} is provided; the
 *       filter can produce a criteria clause consumable by
 *       {@link eu.isygoit.helper.CriteriaHelper}.</li>
 *   <li><b>Manual mode</b> — {@code fieldName} is {@code null}; the
 *       filter notifies an external listener via
 *       {@link #setOnChange(Runnable)} and {@link #toCriteria()} returns
 *       {@code null}.</li>
 * </ul>
 *
 * Dates are formatted ISO-8601 ({@code yyyy-MM-dd}), which is what
 * {@code CriteriaHelper} converts back to {@code LocalDate} / {@code Date}.
 */
public class DateRangeFilter extends HorizontalLayout {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    /** {@code null} in manual mode. */
    private final String fieldName;

    private final DatePicker from = new DatePicker();
    private final DatePicker to   = new DatePicker();

    /** Listener invoked on either picker's value change. */
    private Runnable onChange;

    /** Metadata-mode constructor: sub-labels derive from the group label. */
    public DateRangeFilter(String fieldName, String label) {
        this(fieldName,
                label,
                label + " " + I18n.t("common.filter.date.from"),
                label + " " + I18n.t("common.filter.date.to"));
    }

    /**
     * Full constructor with explicit sub-labels.
     *
     * @param fieldName may be {@code null} for manual / client-side mode
     * @param label     required group label
     * @param fromLabel label for the "from" picker
     * @param toLabel   label for the "to" picker
     */
    public DateRangeFilter(String fieldName, String label,
                           String fromLabel, String toLabel) {
        this.fieldName = fieldName;

        if (label == null) {
            throw new IllegalArgumentException("label must not be null");
        }

        setSpacing(false);
        setPadding(false);
        setAlignItems(Alignment.END);
        addClassName("filter-date-range");

        from.setLabel(fromLabel);
        from.setClearButtonVisible(true);
        from.addClassName("filter-date");
        from.addValueChangeListener(e -> fireOnChange());

        to.setLabel(toLabel);
        to.setClearButtonVisible(true);
        to.addClassName("filter-date");
        to.addValueChangeListener(e -> fireOnChange());

        add(from, to);
    }

    // ══════════════════════════════════════════════════════════════
    //  Manual-mode API
    // ══════════════════════════════════════════════════════════════

    public void setOnChange(Runnable onChange) { this.onChange = onChange; }

    public LocalDate getFromValue() { return from.getValue(); }
    public LocalDate getToValue()   { return to.getValue(); }

    public boolean isEmpty() { return from.isEmpty() && to.isEmpty(); }

    /** Clears both pickers. Does <em>not</em> fire {@code onChange}. */
    public void clear() {
        from.clear();
        to.clear();
    }

    // ══════════════════════════════════════════════════════════════
    //  Metadata-mode API
    // ══════════════════════════════════════════════════════════════

    /**
     * @return the criteria clause for this range, or {@code null} when
     *         in manual mode or no bound is set.
     */
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
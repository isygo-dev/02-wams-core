package eu.isygoit.ui.common.filters;

import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import eu.isygoit.i18n.I18n;

import java.math.BigDecimal;

/**
 * Compact numeric-range filter used in two ways:
 *
 * <ul>
 *   <li><b>Metadata mode</b> — a {@code fieldName} is provided; the
 *       filter produces a criteria clause consumable by
 *       {@link eu.isygoit.helper.CriteriaHelper}.</li>
 *   <li><b>Manual mode</b> — {@code fieldName} is {@code null}; the
 *       filter notifies an external listener via
 *       {@link #setOnChange(Runnable)} and {@link #toCriteria()} returns
 *       {@code null}.</li>
 * </ul>
 * <p>
 * Numeric bounds are emitted without quotes, in {@code BW (lo, hi)}
 * form for a closed interval, or as {@code >=} / {@code <=} for a
 * half-open one. When both bounds are set they are always emitted as
 * {@code [min, max]} so an inverted selection cannot yield an empty
 * result set.
 *
 * <p>All styling is owned by {@code _filter.scss}; this class only
 * publishes the class-name contract ({@code .filter-number-range} on
 * the root, {@code .filter-number} on each field).</p>
 */
public class NumberRangeFilter extends HorizontalLayout {

    /**
     * {@code null} in manual mode.
     */
    private final String fieldName;

    private final NumberField from = new NumberField();
    private final NumberField to = new NumberField();

    /**
     * Listener invoked on either field's value change.
     */
    private Runnable onChange;

    /**
     * Metadata-mode constructor: sub-labels derive from the group label.
     */
    public NumberRangeFilter(String fieldName, String label) {
        this(fieldName,
                label,
                label + " " + I18n.t("common.filter.number.from"),
                label + " " + I18n.t("common.filter.number.to"));
    }

    /**
     * Full constructor with explicit sub-labels.
     *
     * @param fieldName may be {@code null} for manual / client-side mode
     * @param label     required group label
     * @param fromLabel label for the "from" field
     * @param toLabel   label for the "to" field
     */
    public NumberRangeFilter(String fieldName, String label,
                             String fromLabel, String toLabel) {
        this.fieldName = fieldName;

        if (label == null) {
            throw new IllegalArgumentException("label must not be null");
        }

        addClassName("filter-number-range");

        from.setLabel(fromLabel);
        from.setClearButtonVisible(true);
        from.addClassName("filter-number");
        from.addValueChangeListener(e -> fireOnChange());

        to.setLabel(toLabel);
        to.setClearButtonVisible(true);
        to.addClassName("filter-number");
        to.addValueChangeListener(e -> fireOnChange());

        add(from, to);
    }

    // ══════════════════════════════════════════════════════════════
    //  Manual-mode API
    // ══════════════════════════════════════════════════════════════

    /**
     * Formats a numeric bound without a trailing {@code .0} for integral
     * values, and without scientific notation for large/small magnitudes.
     */
    private static String format(double v) {
        if (!Double.isFinite(v)) {
            throw new IllegalArgumentException("Non-finite numeric bound: " + v);
        }
        if (v == Math.floor(v) && Math.abs(v) < 1e15) {
            return Long.toString((long) v);
        }
        return BigDecimal.valueOf(v).stripTrailingZeros().toPlainString();
    }

    public void setOnChange(Runnable onChange) {
        this.onChange = onChange;
    }

    public Double getFromValue() {
        return from.getValue();
    }

    public Double getToValue() {
        return to.getValue();
    }

    public boolean isEmpty() {
        return from.isEmpty() && to.isEmpty();
    }

    // ══════════════════════════════════════════════════════════════
    //  Metadata-mode API
    // ══════════════════════════════════════════════════════════════

    /**
     * Clears both fields. Does <em>not</em> fire {@code onChange}.
     */
    public void clear() {
        from.clear();
        to.clear();
    }

    /**
     * @return the criteria clause for this range, or {@code null} when
     * in manual mode or no bound is set.
     */
    public String toCriteria() {
        if (fieldName == null) return null;

        Double f = from.getValue();
        Double t = to.getValue();

        if (f == null && t == null) return null;

        if (f != null && t != null) {
            double lo = Math.min(f, t);
            double hi = Math.max(f, t);
            return fieldName + " BW (" + format(lo) + ", " + format(hi) + ")";
        }
        if (f != null) return fieldName + " >= " + format(f);
        return fieldName + " <= " + format(t);
    }

    /**
     * True when both bounds are set and {@code from > to}.
     */
    public boolean hasInvalidRange() {
        Double f = from.getValue();
        Double t = to.getValue();
        return f != null && t != null && f > t;
    }

    private void fireOnChange() {
        boolean bad = hasInvalidRange();
        from.setInvalid(bad);
        to.setInvalid(bad);
        if (onChange != null) onChange.run();
    }
}
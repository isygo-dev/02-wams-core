package eu.isygoit.ui.common.filters;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ItemLabelGenerator;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import eu.isygoit.i18n.I18n;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Jira-style filter bar rendered as a card.
 * <p>
 * Layout:
 * <pre>
 *   ┌─ card ──────────────────────────────────────────────────────┐
 *   │                          ( ⌃ )                              │  ← collapse handle, top-center
 *   │  ⚙ Filters                                                  │  ← header
 *   │  ─────────────────────────────────────────────────────────  │
 *   │  [ Group ▾ ]   [ Cycle ▾ ]   [ Status ▾ ]   [du / au]       │  ← controls
 *   │  ─────────────────────────────────────────────────────────  │
 *   │  ACTIVE   [Group: Primary ✕] [Status: Active ✕]   Clear all │  ← chips + link-style clear
 *   └─────────────────────────────────────────────────────────────┘
 * </pre>
 * The chips row hides itself entirely when no filter is set, so a card with
 * no active filters stays compact.
 * <p>
 * A panel only declares its filters; the bar takes care of chips, the
 * "clear all" link, collapsing, and batching the resulting refresh into
 * one call.
 */
public class FilterBar extends Div {

    private final Div controls = new Div();
    private final Div chipsRow = new Div();
    private final Div chipsList = new Div();
    private final Div body = new Div();
    private final Button collapseToggle;
    private final Button clearAllButton;
    private final List<FilterRegistration> registrations = new ArrayList<>();

    private Runnable onRefresh = () -> {
    };
    private boolean suppressRefresh = false;
    private boolean collapsed = false;

    public FilterBar() {
        this(true);
    }

    /**
     * @param startCollapsed when {@code true}, the bar renders with its
     *                       controls and chips already hidden
     */
    public FilterBar(boolean startCollapsed) {
        addClassName("filter-card");

        // ---- collapse handle (top-center, absolutely positioned) ----
        collapseToggle = new Button();
        collapseToggle.setIcon(VaadinIcon.CHEVRON_UP.create());
        collapseToggle.addThemeVariants(
                ButtonVariant.LUMO_TERTIARY_INLINE,
                ButtonVariant.LUMO_SMALL);
        collapseToggle.addClassName("filter-card__collapse");
        collapseToggle.getElement().setAttribute("aria-expanded", "true");
        collapseToggle.getElement().setAttribute("aria-label",
                I18n.t("common.filter.collapse"));
        collapseToggle.addClickListener(e -> toggleCollapsed());

        // ---- header (icon + title, left-aligned) --------------------
        Div header = new Div();
        header.addClassName("filter-card__header");

        Icon filterIcon = VaadinIcon.FILTER.create();
        filterIcon.addClassName("filter-card__icon");

        Span title = new Span(I18n.t("common.filter.title"));
        title.addClassName("filter-card__title");

        header.add(filterIcon, title);

        // ---- body (controls + chips, hidden when collapsed) --------
        controls.addClassName("filter-card__controls");

        chipsRow.addClassName("filter-card__chips");
        chipsRow.setVisible(false);

        Span chipsLabel = new Span(I18n.t("common.filter.active"));
        chipsLabel.addClassName("filter-card__chips-label");

        chipsList.addClassName("filter-card__chips-list");

        clearAllButton = new Button(I18n.t("common.filter.clear"));
        clearAllButton.addThemeVariants(
                ButtonVariant.LUMO_TERTIARY_INLINE,
                ButtonVariant.LUMO_SMALL);
        clearAllButton.addClassName("filter-card__clear");
        clearAllButton.addClickListener(e -> clearAll());

        chipsRow.add(chipsLabel, chipsList, clearAllButton);

        body.addClassName("filter-card__body");
        body.add(controls, chipsRow);

        add(collapseToggle, header, body);

        if (startCollapsed) {
            setCollapsed(true);
        }
    }

    // ==============================================================
    // Public API
    // ==============================================================

    private static String translateStatus(String prefix, StatusFilter status) {
        return switch (status) {
            case ALL -> I18n.t(prefix + ".all");
            case ACTIVE -> I18n.t(prefix + ".active");
            case INACTIVE -> I18n.t(prefix + ".inactive");
        };
    }

    /**
     * Formats a {@code [from, to]} pair for a chip value. Handles open-ended
     * ranges so a single selected bound still produces a readable label.
     */
    private static String formatDateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null) return from + " → " + to;
        if (from != null) return "≥ " + from;
        if (to != null)   return "≤ " + to;
        return "";
    }

    public void setOnRefresh(Runnable onRefresh) {
        this.onRefresh = onRefresh != null ? onRefresh : () -> {
        };
    }

    public boolean isCollapsed() {
        return collapsed;
    }

    /**
     * Collapses or expands the controls + chips block.
     */
    public void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;

        body.setVisible(!collapsed);

        collapseToggle.setIcon((collapsed
                ? VaadinIcon.CHEVRON_DOWN
                : VaadinIcon.CHEVRON_UP).create());
        collapseToggle.getElement().setAttribute("aria-expanded",
                String.valueOf(!collapsed));
        collapseToggle.getElement().setAttribute("aria-label",
                I18n.t(collapsed ? "common.filter.expand" : "common.filter.collapse"));

        setClassName("filter-card--collapsed", collapsed);
    }

    public void toggleCollapsed() {
        setCollapsed(!collapsed);
    }

    /**
     * Adds an enum-backed filter using {@link Enum#name()} as the item label.
     */
    public <E extends Enum<E>> ComboBox<E> addEnumFilter(
            String labelKey, E[] values, Consumer<E> onChange) {
        return addEnumFilter(labelKey, values, Enum::name, onChange);
    }

    /**
     * Adds an enum-backed filter with a custom i18n label generator.
     */
    public <E extends Enum<E>> ComboBox<E> addEnumFilter(
            String labelKey, E[] values,
            ItemLabelGenerator<E> labelGenerator, Consumer<E> onChange) {
        ComboBox<E> combo = new ComboBox<>(I18n.t(labelKey));
        combo.setItems(values);
        registerCombo(combo, I18n.t(labelKey), labelGenerator, onChange,
                v -> v != null,
                null);
        return combo;
    }

    /**
     * Adds a nullable object filter (school year, level, free-text key, …).
     * See the class Javadoc for the {@code Function<? super E, String>} choice.
     */
    public <E> ComboBox<E> addObjectFilter(
            String labelKey, String allPlaceholderKey,
            Function<? super E, String> labelGenerator, Consumer<E> onChange) {
        ComboBox<E> combo = new ComboBox<>(I18n.t(labelKey));
        combo.setPlaceholder(I18n.t(allPlaceholderKey));
        ItemLabelGenerator<E> adapted = labelGenerator::apply;
        registerCombo(combo, I18n.t(labelKey), adapted, onChange,
                v -> v != null,
                null);
        return combo;
    }

    /**
     * Adds a {@link StatusFilter} whose labels come from {@code i18nPrefix.*}.
     */
    public ComboBox<StatusFilter> addStatusFilter(
            String i18nPrefix, Consumer<StatusFilter> onChange) {
        ComboBox<StatusFilter> combo = new ComboBox<>(I18n.t("common.filter.status"));
        combo.setItems(StatusFilter.values());
        combo.setValue(StatusFilter.ALL);
        registerCombo(combo, I18n.t("common.filter.status"),
                s -> translateStatus(i18nPrefix, s),
                v -> onChange.accept(v != null ? v : StatusFilter.ALL),
                v -> v != null && v != StatusFilter.ALL,
                StatusFilter.ALL);
        return combo;
    }

    /**
     * Adds a date-range filter (two {@link DatePicker}s side by side).
     * <p>
     * The filter participates in the chip row and in "clear all", exactly
     * like a combo-backed filter: whenever either picker is set, a chip is
     * emitted with the label {@code labelKey} and a value of the form
     * {@code "2025-01-01 → 2025-12-31"} (or {@code "≥ X"} / {@code "≤ Y"}
     * for open-ended ranges).
     *
     * @param labelKey          i18n key shown on the chip (e.g. "Période")
     * @param fromLabelKey i18n key for the "from" picker placeholder
     * @param toLabelKey   i18n key for the "to" picker placeholder
     * @param onChange          receives the pair whenever either bound
     *                          changes; either argument may be {@code null}
     * @return a handle to the two pickers, for programmatic enable / read /
     *         clear from the owning view
     */
    public DateRangeFilter addDateRangeFilter(
            String labelKey,          // chip label
            String fromLabelKey,      // label for the "from" picker
            String toLabelKey,        // label for the "to" picker
            BiConsumer<LocalDate, LocalDate> onChange) {

        DatePicker from = new DatePicker();
        from.setLabel(I18n.t(fromLabelKey));
        from.addClassName("filter-date");

        DatePicker to = new DatePicker();
        to.setLabel(I18n.t(toLabelKey));
        to.addClassName("filter-date");

        Div wrapper = new Div();
        wrapper.addClassName("filter-date-range");
        wrapper.add(from, to);

        Runnable fire = () -> {
            onChange.accept(from.getValue(), to.getValue());
            rebuildChips();
            if (!suppressRefresh) onRefresh.run();
        };
        from.addValueChangeListener(e -> fire.run());
        to.addValueChangeListener(e -> fire.run());

        registrations.add(new FilterRegistration() {
            @Override public boolean isActive() {
                return from.getValue() != null || to.getValue() != null;
            }
            @Override public void clear() {
                from.clear();
                to.clear();
            }
            @Override public FilterChip toChip(Runnable onClearAll) {
                String value = formatDateRange(from.getValue(), to.getValue());
                return new FilterChip(I18n.t(labelKey), value, onClearAll);
            }
        });

        controls.add(wrapper);

        return new DateRangeFilter(from, to);
    }

    /**
     * Registers an arbitrary custom control (e.g. a bespoke widget).
     * Not part of the chip system — use {@link #addDateRangeFilter} for
     * date ranges that should show up as chips.
     */
    public void addControl(Component c) {
        controls.add(c);
    }

    // ==============================================================
    // Internals
    // ==============================================================

    /**
     * Clears every filter in one shot and triggers a single refresh.
     */
    public void clearAll() {
        suppressRefresh = true;
        try {
            for (FilterRegistration reg : registrations) {
                reg.clear();
            }
            rebuildChips();
        } finally {
            suppressRefresh = false;
        }
        onRefresh.run();
    }

    private <T> void registerCombo(ComboBox<T> combo,
                                   String label,
                                   ItemLabelGenerator<T> generator,
                                   Consumer<T> onChange,
                                   Predicate<T> isActiveValue,
                                   T clearedValue) {
        combo.setItemLabelGenerator(generator);
        combo.setClearButtonVisible(clearedValue == null);
        combo.addClassName("filter-select");

        registrations.add(new ComboFilterRegistration<>(
                combo, label, generator, isActiveValue, clearedValue));

        combo.addValueChangeListener(e -> {
            onChange.accept(e.getValue());
            rebuildChips();
            if (!suppressRefresh) onRefresh.run();
        });

        controls.add(combo);
    }

    private void rebuildChips() {
        chipsList.removeAll();

        boolean any = false;
        for (FilterRegistration reg : registrations) {
            if (reg.isActive()) {
                chipsList.add(reg.toChip(this::clearAll));
                any = true;
            }
        }
        chipsRow.setVisible(any);

        // Lets the SCSS hint "there are filters" while collapsed.
        setClassName("filter-card--has-active", any);
    }

    // ==============================================================
    // Nested types
    // ==============================================================

    /** Uniform contract for anything that can produce a chip and be cleared. */
    private interface FilterRegistration {
        boolean isActive();
        void clear();
        FilterChip toChip(Runnable onClearAll);
    }

    /** A {@link ComboBox}-backed filter. */
    private static final class ComboFilterRegistration<T> implements FilterRegistration {
        final ComboBox<T> combo;
        final String label;
        final ItemLabelGenerator<T> labelGenerator;
        final Predicate<T> isActiveValue;
        final T clearedValue;

        ComboFilterRegistration(ComboBox<T> combo,
                                String label,
                                ItemLabelGenerator<T> generator,
                                Predicate<T> isActiveValue,
                                T clearedValue) {
            this.combo = combo;
            this.label = label;
            this.labelGenerator = generator;
            this.isActiveValue = isActiveValue;
            this.clearedValue = clearedValue;
        }

        @Override
        public boolean isActive() {
            return isActiveValue.test(combo.getValue());
        }

        @Override
        public void clear() {
            combo.setValue(clearedValue);
        }

        @Override
        public FilterChip toChip(Runnable onClearAll) {
            T v = combo.getValue();
            String text = labelGenerator != null
                    ? labelGenerator.apply(v)
                    : String.valueOf(v);
            return new FilterChip(label, text, onClearAll);
        }
    }

    /**
     * Handle returned by {@link #addDateRangeFilter} — lets the owning view
     * read, clear, or enable / disable the two pickers without exposing the
     * internal {@link DatePicker} instances directly.
     */
    public static final class DateRangeFilter {
        private final DatePicker from;
        private final DatePicker to;

        DateRangeFilter(DatePicker from, DatePicker to) {
            this.from = from;
            this.to = to;
        }

        public LocalDate getFromValue() { return from.getValue(); }
        public LocalDate getToValue()   { return to.getValue(); }

        public void clear() {
            from.clear();
            to.clear();
        }

        public void setEnabled(boolean enabled) {
            from.setEnabled(enabled);
            to.setEnabled(enabled);
        }
    }
}
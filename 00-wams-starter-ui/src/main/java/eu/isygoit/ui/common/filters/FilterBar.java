package eu.isygoit.ui.common.filters;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.enums.IEnum;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.component.BidiLtr;
import eu.isygoit.ui.common.component.LocaleAwareDateFormat;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Collapsible, chip-aware filter bar supporting two usage modes.
 *
 * <ul>
 *   <li><b>Metadata mode</b> — constructed with the map from
 *       {@code getAnnotatedCriteria()}. The control type per field is
 *       inferred from the field's type name; enums are resolved through
 *       either a per-field hint map or a simple-name registry.</li>
 *   <li><b>Manual mode</b> — fields added programmatically via
 *       {@code addTextFilter}, {@code addEnumFilter}, etc.</li>
 * </ul>
 *
 * <p>Boolean criteria render as a {@link ToggleSwitch} — a tri-state
 * switch whose values are {@code null} (no filter), {@code true},
 * and {@code false}.</p>
 *
 * <p><b>Layout:</b> the header hosts the title, the active-count badge,
 * and the {@code Apply} / {@code Clear} actions on the right. The body
 * hosts only the filter controls and the active-filter chips row. All
 * sizing / layout / visuals live in {@code _filter.scss}; the only
 * contract exposed to CSS is the class-name set emitted here.</p>
 */
@Slf4j
public class FilterBar extends Div {

    // ─── Metadata-mode state ───────────────────────────────────────
    private final Map<String, String> criteriaMetadata;
    private final Map<String, Component> fieldInputs = new LinkedHashMap<>();

    private final Map<String, Class<? extends Enum<?>>> enumRegistry = new LinkedHashMap<>();
    private final Map<String, Class<? extends Enum<?>>> fieldEnumHints = new LinkedHashMap<>();

    // ─── Manual-mode state ─────────────────────────────────────────
    private final List<Runnable> manualClearers = new ArrayList<>();

    // ─── Chip tracking ─────────────────────────────────────────────
    private final Map<String, ChipEntry> chips = new LinkedHashMap<>();

    // ─── UI nodes ──────────────────────────────────────────────────
    private final Button collapseButton = new Button();
    private final Div header = new Div();
    private final Div headerActions = new Div();
    private final Div body = new Div();
    private final Div controls = new Div();
    private final Div chipsRow = new Div();
    private final Div chipsList = new Div();
    private final Span activeCount = new Span();
    private final Button applyButton = new Button();
    private final Button clearButton = new Button();

    // ─── Callbacks ─────────────────────────────────────────────────
    private Consumer<String> onApply;
    private Runnable onClear;

    private boolean collapsed = true;

    public FilterBar() {
        this(Map.of(), List.of(), Map.of());
    }

    // ══════════════════════════════════════════════════════════════
    //  Constructors
    // ══════════════════════════════════════════════════════════════

    public FilterBar(Map<String, String> criteriaMetadata) {
        this(criteriaMetadata, List.of(), Map.of());
    }

    public FilterBar(Map<String, String> criteriaMetadata,
                     Collection<Class<? extends Enum<?>>> enumTypes) {
        this(criteriaMetadata, enumTypes, Map.of());
    }

    public FilterBar(Map<String, String> criteriaMetadata,
                     Collection<Class<? extends Enum<?>>> enumTypes,
                     Map<String, Class<? extends Enum<?>>> fieldEnumHints) {
        this.criteriaMetadata = criteriaMetadata != null ? criteriaMetadata : Map.of();

        if (enumTypes != null) {
            for (Class<? extends Enum<?>> t : enumTypes) {
                if (t != null && t.isEnum()) enumRegistry.put(t.getSimpleName(), t);
            }
        }
        if (fieldEnumHints != null) {
            for (var e : fieldEnumHints.entrySet()) {
                if (e.getValue() != null && e.getValue().isEnum()) {
                    this.fieldEnumHints.put(e.getKey(), e.getValue());
                }
            }
        }
        build();
    }

    /**
     * Formats a date for the chip/badge text, kept left-to-right even on a right-to-left page.
     */
    private static String displayDate(LocalDate date) {
        return BidiLtr.wrap(LocaleAwareDateFormat.format(date));
    }

    private static String formatRange(LocalDate from, LocalDate to) {
        if (from == null && to == null) return null;
        if (from != null && to != null)
            return displayDate(from) + " → " + displayDate(to);
        if (from != null) return "≥ " + displayDate(from);
        return "≤ " + displayDate(to);
    }

    // ══════════════════════════════════════════════════════════════
    //  Public callbacks
    // ══════════════════════════════════════════════════════════════

    public void setOnApply(Consumer<String> onApply) {
        this.onApply = onApply;
    }

    public void setOnClear(Runnable onClear) {
        this.onClear = onClear;
    }

    public void setOnRefresh(Runnable onRefresh) {
        setOnApply(criteria -> onRefresh.run());
    }

    // ══════════════════════════════════════════════════════════════
    //  Manual mode — text / number / boolean
    // ══════════════════════════════════════════════════════════════

    public TextField addTextFilter(String labelKey, Consumer<String> onChange) {
        TextField field = new TextField(I18n.t(labelKey));
        field.setClearButtonVisible(true);
        field.addClassName("filter-select");
        field.addClassName("filter-control");

        String key = "text:" + labelKey;
        field.addValueChangeListener(e -> {
            String v = e.getValue();
            if (onChange != null) onChange.accept(v);
            trackChip(key, I18n.t(labelKey), v, field::clear);
        });
        registerManualField(field::clear);
        return field;
    }

    public IntegerField addIntegerFilter(String labelKey, Consumer<Integer> onChange) {
        IntegerField field = new IntegerField(I18n.t(labelKey));
        field.setClearButtonVisible(true);
        field.setStepButtonsVisible(true);
        field.addClassName("filter-select");
        field.addClassName("filter-number");
        field.addClassName("filter-control");

        String key = "int:" + labelKey;
        field.addValueChangeListener(e -> {
            Integer v = e.getValue();
            if (onChange != null) onChange.accept(v);
            trackChip(key, I18n.t(labelKey),
                    v == null ? null : v.toString(), field::clear);
        });
        registerManualField(field::clear);
        return field;
    }

    public NumberField addNumberFilter(String labelKey, Consumer<Double> onChange) {
        NumberField field = new NumberField(I18n.t(labelKey));
        field.setClearButtonVisible(true);
        field.setStepButtonsVisible(true);
        field.addClassName("filter-select");
        field.addClassName("filter-number");
        field.addClassName("filter-control");

        String key = "num:" + labelKey;
        field.addValueChangeListener(e -> {
            Double v = e.getValue();
            if (onChange != null) onChange.accept(v);
            trackChip(key, I18n.t(labelKey),
                    v == null ? null : v.toString(), field::clear);
        });
        registerManualField(field::clear);
        return field;
    }

    /**
     * Boolean filter rendered as a {@link ToggleSwitch}.
     * Cycles: unset → true → false → unset.
     */
    public ToggleSwitch addBooleanFilter(String labelKey, Consumer<Boolean> onChange) {
        ToggleSwitch toggle = new ToggleSwitch(I18n.t(labelKey));
        String key = "bool:" + labelKey;

        toggle.setOnChange(v -> {
            if (onChange != null) onChange.accept(v);
            String chipValue = v == null ? null : (Boolean.TRUE.equals(v) ? "✓" : "✗");
            trackChip(key, I18n.t(labelKey), chipValue, toggle::clear);
        });
        registerManualField(toggle::clear);
        return toggle;
    }

    // ══════════════════════════════════════════════════════════════
    //  Manual mode — dates
    // ══════════════════════════════════════════════════════════════

    public DatePicker addDateFilter(String labelKey, Consumer<LocalDate> onChange) {
        DatePicker picker = new DatePicker(I18n.t(labelKey));
        picker.setClearButtonVisible(true);
        picker.addClassName("filter-date");
        picker.addClassName("filter-control");

        String key = "date:" + labelKey;
        picker.addValueChangeListener(e -> {
            LocalDate v = e.getValue();
            if (onChange != null) onChange.accept(v);
            trackChip(key, I18n.t(labelKey),
                    v == null ? null : displayDate(v), picker::clear);
        });
        registerManualField(picker::clear);
        return picker;
    }

    public DateTimePicker addDateTimeFilter(String labelKey, Consumer<LocalDateTime> onChange) {
        DateTimePicker picker = new DateTimePicker(I18n.t(labelKey));
        picker.addClassName("filter-date");
        picker.addClassName("filter-control");

        String key = "dt:" + labelKey;
        picker.addValueChangeListener(e -> {
            LocalDateTime v = e.getValue();
            if (onChange != null) onChange.accept(v);
            String formatted = v == null ? null : displayDate(v.toLocalDate());
            trackChip(key, I18n.t(labelKey), formatted, () -> picker.setValue(null));
        });
        registerManualField(() -> picker.setValue(null));
        return picker;
    }

    public DateRangeFilter addDateRangeFilter(
            String labelKey, String fromKey, String toKey,
            BiConsumer<LocalDate, LocalDate> onChange) {

        DateRangeFilter filter = new DateRangeFilter(
                null, I18n.t(labelKey), I18n.t(fromKey), I18n.t(toKey));

        String key = "range:" + labelKey;
        filter.setOnChange(() -> {
            LocalDate f = filter.getFromValue();
            LocalDate t = filter.getToValue();
            if (onChange != null) onChange.accept(f, t);
            trackChip(key, I18n.t(labelKey), formatRange(f, t), filter::clear);
        });

        // No more anchor button in `controls` — append directly.
        controls.add(filter);
        manualClearers.add(filter::clear);
        setVisible(true);
        return filter;
    }

    // ══════════════════════════════════════════════════════════════
    //  Manual mode — combo boxes
    // ══════════════════════════════════════════════════════════════

    public <E extends Enum<E>> ComboBox<E> addEnumFilter(
            String labelKey, E[] values,
            Function<E, String> labeler, Consumer<E> onChange) {

        ComboBox<E> combo = new ComboBox<>();
        combo.setLabel(I18n.t(labelKey));
        combo.setItems(values);
        combo.setItemLabelGenerator(labeler::apply);
        combo.setClearButtonVisible(true);
        combo.addClassName("filter-select");
        combo.addClassName("filter-control");

        String key = "enum:" + labelKey;
        combo.addValueChangeListener(e -> {
            E v = e.getValue();
            if (onChange != null) onChange.accept(v);
            trackChip(key, I18n.t(labelKey),
                    v == null ? null : labeler.apply(v), combo::clear);
        });
        registerManualField(combo::clear);
        return combo;
    }

    public <E extends Enum<E>> ComboBox<E> addEnumFilter(
            String labelKey, E[] values, Consumer<E> onChange) {
        return addEnumFilter(labelKey, values,
                e -> I18n.t(labelKey + "." + e.name().toLowerCase()), onChange);
    }

    public <E> ComboBox<E> addObjectFilter(
            String labelKey, String allOptionKey,
            Function<E, String> labeler, Consumer<E> onChange) {

        ComboBox<E> combo = new ComboBox<>();
        combo.setLabel(I18n.t(labelKey));
        if (allOptionKey != null) combo.setPlaceholder(I18n.t(allOptionKey));
        if (labeler != null) combo.setItemLabelGenerator(labeler::apply);
        combo.setClearButtonVisible(true);
        combo.addClassName("filter-select");
        combo.addClassName("filter-control");

        String key = "object:" + labelKey;
        combo.addValueChangeListener(e -> {
            E v = e.getValue();
            if (onChange != null) onChange.accept(v);
            String display = v == null
                    ? null
                    : (labeler != null ? labeler.apply(v) : String.valueOf(v));
            trackChip(key, I18n.t(labelKey), display, combo::clear);
        });
        registerManualField(combo::clear);
        return combo;
    }

    public ComboBox<StatusFilter> addStatusFilter(
            String labelKeyPrefix, Consumer<StatusFilter> onChange) {

        ComboBox<StatusFilter> combo = new ComboBox<>();
        combo.setLabel(I18n.t(labelKeyPrefix));
        combo.setItems(StatusFilter.values());
        combo.setItemLabelGenerator(s ->
                I18n.t(labelKeyPrefix + "." + s.name().toLowerCase()));
        combo.setValue(StatusFilter.ALL);
        combo.setClearButtonVisible(false);
        combo.addClassName("filter-select");
        combo.addClassName("filter-control");

        String key = "status:" + labelKeyPrefix;
        combo.addValueChangeListener(e -> {
            StatusFilter v = e.getValue() == null ? StatusFilter.ALL : e.getValue();
            if (onChange != null) onChange.accept(v);
            String display = v == StatusFilter.ALL ? null
                    : I18n.t(labelKeyPrefix + "." + v.name().toLowerCase());
            trackChip(key, I18n.t(labelKeyPrefix), display,
                    () -> combo.setValue(StatusFilter.ALL));
        });
        registerManualField(() -> combo.setValue(StatusFilter.ALL));
        return combo;
    }

    // ══════════════════════════════════════════════════════════════
    //  Manual mode — arbitrary control
    // ══════════════════════════════════════════════════════════════

    public Component addControl(Component control) {
        return addControl(control, null);
    }

    public Component addControl(Component control, Runnable onClearAction) {
        if (control == null) return null;
        controls.add(control);
        if (onClearAction != null) manualClearers.add(onClearAction);
        setVisible(true);
        return control;
    }

    // ══════════════════════════════════════════════════════════════
    //  Layout construction
    // ══════════════════════════════════════════════════════════════

    private void build() {
        addClassName("filter-card");

        // ── Collapse pill ──────────────────────────────────────────
        collapseButton.addClassName("filter-card__collapse");
        collapseButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        collapseButton.setIcon(
                (collapsed ? VaadinIcon.CHEVRON_DOWN : VaadinIcon.CHEVRON_UP).create());
        collapseButton.getElement().setAttribute("aria-label", I18n.t("common.filter.toggle"));
        collapseButton.addClickListener(e -> toggleCollapsed());
        add(collapseButton);

        // ── Header: icon · title · count · [spacer] · actions ─────
        header.addClassName("filter-card__header");

        Icon filterIcon = VaadinIcon.FILTER.create();
        filterIcon.addClassName("filter-card__icon");

        Span titleSpan = new Span(I18n.t("common.filter.title"));
        titleSpan.addClassName("filter-card__title");

        activeCount.addClassName("filter-card__count");

        applyButton.setText(I18n.t("common.filter.apply"));
        applyButton.setIcon(VaadinIcon.FILTER.create());
        applyButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        applyButton.addClassName("filter-card__apply");
        applyButton.addClickListener(e -> fireApply());

        clearButton.setText(I18n.t("common.filter.clear"));
        clearButton.setIcon(VaadinIcon.ERASER.create());
        clearButton.addClassName("filter-card__clear-btn");
        clearButton.addClickListener(e -> fireClear());

        headerActions.addClassName("filter-card__actions");
        headerActions.add(applyButton, clearButton);

        header.add(filterIcon, titleSpan, activeCount, headerActions);
        add(header);

        // ── Body: controls + chips ────────────────────────────────
        body.addClassName("filter-card__body");

        controls.addClassName("filter-card__controls");
        for (var entry : criteriaMetadata.entrySet()) {
            Component input = createInput(entry.getKey(), entry.getValue());
            fieldInputs.put(entry.getKey(), input);
            controls.add(input);
        }

        chipsRow.addClassName("filter-card__chips");
        Span chipsLabel = new Span(I18n.t("common.filter.active"));
        chipsLabel.addClassName("filter-card__chips-label");
        chipsList.addClassName("filter-card__chips-list");
        chipsRow.add(chipsLabel, chipsList);

        body.add(controls, chipsRow);
        add(body);

        // ── Initial visibility / state ────────────────────────────
        body.setVisible(!collapsed);
        getElement().getClassList().set("filter-card--collapsed", collapsed);

        chipsRow.setVisible(false);
        activeCount.setVisible(false);
        getElement().getClassList().set("filter-card--has-active", false);
        setVisible(!fieldInputs.isEmpty());
    }

    private void toggleCollapsed() {
        collapsed = !collapsed;
        body.setVisible(!collapsed);
        getElement().getClassList().set("filter-card--collapsed", collapsed);
        collapseButton.setIcon(
                (collapsed ? VaadinIcon.CHEVRON_DOWN : VaadinIcon.CHEVRON_UP).create());
    }

    private void registerManualField(Runnable onClear) {
        if (onClear != null) manualClearers.add(onClear);
        setVisible(true);
    }

    // ══════════════════════════════════════════════════════════════
    //  Metadata-mode input factory
    // ══════════════════════════════════════════════════════════════

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Component createInput(String name, String type) {
        String label = resolveLabel(name);
        String t = type == null ? "String" : type;
        String key = "meta:" + name;

        Class<? extends Enum<?>> enumType = fieldEnumHints.get(name);
        if (enumType == null) enumType = enumRegistry.get(t);
        if (enumType != null) {
            ComboBox combo = new ComboBox();
            combo.setLabel(label);
            combo.setItems(enumType.getEnumConstants());
            combo.setItemLabelGenerator(v -> labelForEnum(name, (Enum<?>) v));
            combo.setClearButtonVisible(true);
            combo.addClassName("filter-select");
            combo.addClassName("filter-control");
            combo.addValueChangeListener(e -> {
                Enum<?> v = (Enum<?>) e.getValue();
                trackChip(key, label,
                        v == null ? null : labelForEnum(name, v),
                        combo::clear);
            });
            return combo;
        }

        return switch (t) {
            case "Integer", "int" -> {
                IntegerField f = new IntegerField(label);
                f.setClearButtonVisible(true);
                f.setStepButtonsVisible(true);
                f.addClassName("filter-select");
                f.addClassName("filter-number");
                f.addClassName("filter-control");
                f.addValueChangeListener(e ->
                        trackChip(key, label,
                                e.getValue() == null ? null : e.getValue().toString(),
                                f::clear));
                yield f;
            }
            case "Long", "long", "Double", "double", "Float", "float", "BigDecimal" -> {
                NumberField f = new NumberField(label);
                f.setClearButtonVisible(true);
                f.setStepButtonsVisible(true);
                f.addClassName("filter-select");
                f.addClassName("filter-number");
                f.addClassName("filter-control");
                f.addValueChangeListener(e ->
                        trackChip(key, label,
                                e.getValue() == null ? null : e.getValue().toString(),
                                f::clear));
                yield f;
            }
            // ── Boolean → ToggleSwitch ────────────────────────────
            case "Boolean", "boolean" -> {
                ToggleSwitch ts = new ToggleSwitch(label);
                ts.setOnChange(v -> {
                    String chipValue = v == null ? null
                            : (Boolean.TRUE.equals(v) ? "✓" : "✗");
                    trackChip(key, label, chipValue, ts::clear);
                });
                yield ts;
            }
            case "LocalDate", "Date", "java.sql.Date" -> {
                DateRangeFilter drf = new DateRangeFilter(name, label);
                drf.setOnChange(() ->
                        trackChip(key, label,
                                formatRange(drf.getFromValue(), drf.getToValue()),
                                drf::clear));
                yield drf;
            }
            case "LocalDateTime", "Instant", "OffsetDateTime" -> {
                DateTimePicker f = new DateTimePicker(label);
                f.addClassName("filter-date");
                f.addClassName("filter-control");
                f.addValueChangeListener(e ->
                        trackChip(key, label,
                                e.getValue() == null ? null
                                        : displayDate(e.getValue().toLocalDate()),
                                () -> f.setValue(null)));
                yield f;
            }
            default -> {
                TextField f = new TextField(label);
                f.setClearButtonVisible(true);
                f.addClassName("filter-select");
                f.addClassName("filter-control");
                f.addValueChangeListener(e ->
                        trackChip(key, label, e.getValue(), f::clear));
                yield f;
            }
        };
    }

    private String labelForEnum(String fieldName, Enum<?> value) {
        String i18nKey = "filter.field." + fieldName + "." + value.name().toLowerCase();
        String translated = I18n.t(i18nKey);
        if (!translated.equals(i18nKey)) return translated;
        if (value instanceof IEnum ienum) return ienum.meaning();
        return value.name();
    }

    private String resolveLabel(String name) {
        String key = "filter.field." + name;
        String translated = I18n.t(key);
        return translated.equals(key) ? humanize(name) : translated;
    }

    private String humanize(String name) {
        if (name == null || name.isEmpty()) return "";
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    // ══════════════════════════════════════════════════════════════
    //  Chips
    // ══════════════════════════════════════════════════════════════

    private void trackChip(String key, String label, String value, Runnable onRemove) {
        if (value == null || value.isBlank()) {
            chips.remove(key);
        } else {
            chips.put(key, new ChipEntry(label, value, onRemove));
        }
        refreshChips();
    }

    private void refreshChips() {
        chipsList.removeAll();
        for (ChipEntry entry : chips.values()) {
            chipsList.add(new FilterChip(entry.label(), entry.value(), () -> {
                if (entry.onRemove() != null) entry.onRemove().run();
            }));
        }
        boolean hasChips = !chips.isEmpty();
        chipsRow.setVisible(hasChips);
        activeCount.setText(String.valueOf(chips.size()));
        activeCount.setVisible(hasChips);
        getElement().getClassList().set("filter-card--has-active", hasChips);
    }

    private void fireApply() {
        if (onApply != null) onApply.accept(buildCriteriaString());
    }

    // ══════════════════════════════════════════════════════════════
    //  Apply / Clear
    // ══════════════════════════════════════════════════════════════

    private void fireClear() {
        fieldInputs.values().forEach(this::clearInput);
        manualClearers.forEach(Runnable::run);
        chips.clear();
        refreshChips();
        if (onClear != null) onClear.run();
    }

    private void clearInput(Component c) {
        if (c instanceof ToggleSwitch ts) ts.clear();
        else if (c instanceof DateRangeFilter drf) drf.clear();
        else if (c instanceof TextField tf) tf.clear();
        else if (c instanceof IntegerField f) f.clear();
        else if (c instanceof NumberField f) f.clear();
        else if (c instanceof DatePicker f) f.clear();
        else if (c instanceof DateTimePicker f) f.setValue(null);
        else if (c instanceof ComboBox<?> f) f.clear();
    }

    private String buildCriteriaString() {
        StringBuilder sb = new StringBuilder();

        for (var entry : fieldInputs.entrySet()) {
            Component comp = entry.getValue();
            String clause;

            if (comp instanceof DateRangeFilter drf) {
                clause = drf.toCriteria();
            } else {
                Object raw = getInputValue(comp);
                if (raw == null) continue;
                String str = raw.toString().trim();
                if (str.isEmpty()) continue;
                clause = buildClause(entry.getKey(),
                        criteriaMetadata.get(entry.getKey()), str);
            }

            if (clause == null || clause.isBlank()) continue;
            if (sb.length() > 0) sb.append(" & ");
            sb.append(clause);
        }
        return sb.toString();
    }

    // ══════════════════════════════════════════════════════════════
    //  Criteria string (metadata mode)
    // ══════════════════════════════════════════════════════════════

    private String buildClause(String field, String type, String raw) {
        if ("Boolean".equals(type) || "boolean".equals(type)) {
            if ("true".equals(raw) || "false".equals(raw)) {
                return field + " = '" + raw + "'";
            }
            return null;
        }

        String op;
        String value;

        String[] prefixes = {">=", "<=", "!=", ">", "<"};
        String matched = null;
        for (String p : prefixes) {
            if (raw.startsWith(p)) {
                matched = p;
                break;
            }
        }

        if (matched != null) {
            op = matched;
            value = raw.substring(matched.length()).trim();
        } else if ("String".equals(type)) {
            op = "~";
            value = raw;
        } else {
            op = "=";
            value = raw;
        }

        if (value.isEmpty()) return null;
        value = value.replace("'", "''");
        return field + " " + op + " '" + value + "'";
    }

    private Object getInputValue(Component c) {
        if (c instanceof ToggleSwitch ts) return ts.getValue();
        if (c instanceof TextField tf) return tf.getValue();
        if (c instanceof IntegerField f) return f.getValue();
        if (c instanceof NumberField f) return f.getValue();
        if (c instanceof DatePicker f) return f.getValue();
        if (c instanceof DateTimePicker f) return f.getValue();
        if (c instanceof ComboBox<?> f) return f.getValue();
        return null;
    }

    private record ChipEntry(String label, String value, Runnable onRemove) {
    }
}
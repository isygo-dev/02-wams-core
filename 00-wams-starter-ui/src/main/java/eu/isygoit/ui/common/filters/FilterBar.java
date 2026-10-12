package eu.isygoit.ui.common.filters;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
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
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Collapsible, chip-aware filter bar supporting two usage modes.
 *
 * <ul>
 *   <li><b>Metadata mode</b> — {@code new FilterBar(getAnnotatedCriteria())}
 *       builds one control per annotated field. The control type is
 *       inferred from the field's type name; enums are matched against a
 *       per-view registry of enum classes.
 *       Apply emits a criteria string consumable by the backend
 *       {@code /filter} endpoint.</li>
 *   <li><b>Manual mode</b> — {@code new FilterBar()} followed by
 *       {@code addTextFilter}, {@code addEnumFilter},
 *       {@code addDateRangeFilter}, etc. Each control notifies its own
 *       listener; Apply fires a refresh.</li>
 * </ul>
 *
 * <p>Every active filter is mirrored as a removable chip in the bottom
 * strip. Clearing a chip clears the corresponding control, which in turn
 * removes the chip — the two stay in sync through the normal value-change
 * listeners.</p>
 */
@Slf4j
public class FilterBar extends Div {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    // ─── Metadata-mode state ───────────────────────────────────────
    private final Map<String, String> criteriaMetadata;
    private final Map<String, Component> fieldInputs = new LinkedHashMap<>();
    private final Map<String, Class<? extends Enum<?>>> enumRegistry = new LinkedHashMap<>();

    // ─── Manual-mode state ─────────────────────────────────────────
    private final List<Runnable> manualClearers = new ArrayList<>();

    // ─── Chip tracking — key → chip ────────────────────────────────
    private final Map<String, ChipEntry> chips = new LinkedHashMap<>();

    // ─── UI nodes ──────────────────────────────────────────────────
    private final Button collapseButton = new Button();
    private final Div header = new Div();
    private final Div body = new Div();
    private final Div controls = new Div();
    private final Div chipsRow = new Div();
    private final Div chipsList = new Div();
    private final Span activeCount = new Span();
    private final Button applyButton = new Button();
    private final Button clearButton = new Button();
    private final Button clearAllLink = new Button();

    // ─── Callbacks ─────────────────────────────────────────────────
    private Consumer<String> onApply;
    private Runnable onClear;

    // ─── Layout state ──────────────────────────────────────────────
    private boolean collapsed = false;

    private record ChipEntry(String label, String value, Runnable onRemove) {}

    // ══════════════════════════════════════════════════════════════
    //  Constructors
    // ══════════════════════════════════════════════════════════════

    public FilterBar() {
        this(Map.of(), List.of());
    }

    public FilterBar(Map<String, String> criteriaMetadata) {
        this(criteriaMetadata, List.of());
    }

    /**
     * Full constructor with enum support.
     * <p>
     * Any metadata entry whose type name matches the simple name of one
     * of {@code enumTypes} is rendered as a ComboBox instead of a text
     * field — no per-field hints needed.
     *
     * @param criteriaMetadata the map from {@code getAnnotatedCriteria()}
     * @param enumTypes        the enum classes the DTO can contain; may
     *                         be empty, in which case the behaviour is
     *                         identical to {@link #FilterBar(Map)}
     */
    public FilterBar(Map<String, String> criteriaMetadata,
                     Collection<Class<? extends Enum<?>>> enumTypes) {
        this.criteriaMetadata = criteriaMetadata != null ? criteriaMetadata : Map.of();
        if (enumTypes != null) {
            for (Class<? extends Enum<?>> t : enumTypes) {
                if (t != null && t.isEnum()) {
                    enumRegistry.put(t.getSimpleName(), t);
                }
            }
        }
        build();
    }

    // ══════════════════════════════════════════════════════════════
    //  Public callbacks
    // ══════════════════════════════════════════════════════════════

    public void setOnApply(Consumer<String> onApply) { this.onApply = onApply; }
    public void setOnClear(Runnable onClear)         { this.onClear = onClear; }

    /** Alias: fires on Apply but ignores the criteria string. */
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
        field.setWidth("14em");

        String key = "text:" + labelKey;
        field.addValueChangeListener(e -> {
            String v = e.getValue();
            if (onChange != null) onChange.accept(v);
            trackChip(key, I18n.t(labelKey), v, field::clear);
        });
        registerManualField(field, field::clear);
        return field;
    }

    public IntegerField addIntegerFilter(String labelKey, Consumer<Integer> onChange) {
        IntegerField field = new IntegerField(I18n.t(labelKey));
        field.setClearButtonVisible(true);
        field.setStepButtonsVisible(true);
        field.addClassName("filter-select");
        field.setWidth("10em");

        String key = "int:" + labelKey;
        field.addValueChangeListener(e -> {
            Integer v = e.getValue();
            if (onChange != null) onChange.accept(v);
            trackChip(key, I18n.t(labelKey),
                    v == null ? null : v.toString(), field::clear);
        });
        registerManualField(field, field::clear);
        return field;
    }

    public NumberField addNumberFilter(String labelKey, Consumer<Double> onChange) {
        NumberField field = new NumberField(I18n.t(labelKey));
        field.setClearButtonVisible(true);
        field.setStepButtonsVisible(true);
        field.addClassName("filter-select");
        field.setWidth("10em");

        String key = "num:" + labelKey;
        field.addValueChangeListener(e -> {
            Double v = e.getValue();
            if (onChange != null) onChange.accept(v);
            trackChip(key, I18n.t(labelKey),
                    v == null ? null : v.toString(), field::clear);
        });
        registerManualField(field, field::clear);
        return field;
    }

    public Checkbox addBooleanFilter(String labelKey, Consumer<Boolean> onChange) {
        Checkbox cb = new Checkbox(I18n.t(labelKey));
        cb.addClassName("filter-checkbox");

        String key = "bool:" + labelKey;
        cb.addValueChangeListener(e -> {
            Boolean v = e.getValue();
            if (onChange != null) onChange.accept(v);
            String chipValue = Boolean.TRUE.equals(v) ? "✓" : null;
            trackChip(key, I18n.t(labelKey), chipValue, () -> cb.setValue(false));
        });
        registerManualField(cb, () -> cb.setValue(false));
        return cb;
    }

    // ══════════════════════════════════════════════════════════════
    //  Manual mode — dates
    // ══════════════════════════════════════════════════════════════

    public DatePicker addDateFilter(String labelKey, Consumer<LocalDate> onChange) {
        DatePicker picker = new DatePicker(I18n.t(labelKey));
        picker.setClearButtonVisible(true);
        picker.addClassName("filter-date");

        String key = "date:" + labelKey;
        picker.addValueChangeListener(e -> {
            LocalDate v = e.getValue();
            if (onChange != null) onChange.accept(v);
            trackChip(key, I18n.t(labelKey),
                    v == null ? null : v.format(DATE_FMT), picker::clear);
        });
        registerManualField(picker, picker::clear);
        return picker;
    }

    public DateTimePicker addDateTimeFilter(String labelKey, Consumer<LocalDateTime> onChange) {
        DateTimePicker picker = new DateTimePicker(I18n.t(labelKey));
        picker.addClassName("filter-date");

        String key = "dt:" + labelKey;
        picker.addValueChangeListener(e -> {
            LocalDateTime v = e.getValue();
            if (onChange != null) onChange.accept(v);
            String formatted = v == null ? null : v.toLocalDate().format(DATE_FMT);
            trackChip(key, I18n.t(labelKey), formatted, () -> picker.setValue(null));
        });
        registerManualField(picker, () -> picker.setValue(null));
        return picker;
    }

    public DateRangeFilter addDateRangeFilter(
            String labelKey,
            String fromKey,
            String toKey,
            BiConsumer<LocalDate, LocalDate> onChange) {

        DateRangeFilter filter = new DateRangeFilter(
                null,
                I18n.t(labelKey),
                I18n.t(fromKey),
                I18n.t(toKey));

        String key = "range:" + labelKey;
        filter.setOnChange(() -> {
            LocalDate f = filter.getFromValue();
            LocalDate t = filter.getToValue();
            if (onChange != null) onChange.accept(f, t);
            trackChip(key, I18n.t(labelKey), formatRange(f, t), filter::clear);
        });

        insertBefore(filter, applyButton);
        manualClearers.add(filter::clear);
        setVisible(true);
        return filter;
    }

    // ══════════════════════════════════════════════════════════════
    //  Manual mode — combo boxes
    // ══════════════════════════════════════════════════════════════

    public <E extends Enum<E>> ComboBox<E> addEnumFilter(
            String labelKey,
            E[] values,
            Function<E, String> labeler,
            Consumer<E> onChange) {

        ComboBox<E> combo = new ComboBox<>();
        combo.setLabel(I18n.t(labelKey));
        combo.setItems(values);
        combo.setItemLabelGenerator(labeler::apply);
        combo.setClearButtonVisible(true);
        combo.addClassName("filter-select");

        String key = "enum:" + labelKey;
        combo.addValueChangeListener(e -> {
            E v = e.getValue();
            if (onChange != null) onChange.accept(v);
            trackChip(key, I18n.t(labelKey),
                    v == null ? null : labeler.apply(v), combo::clear);
        });
        registerManualField(combo, combo::clear);
        return combo;
    }

    public <E extends Enum<E>> ComboBox<E> addEnumFilter(
            String labelKey,
            E[] values,
            Consumer<E> onChange) {
        return addEnumFilter(
                labelKey,
                values,
                e -> I18n.t(labelKey + "." + e.name().toLowerCase()),
                onChange);
    }

    public <E> ComboBox<E> addObjectFilter(
            String labelKey,
            String allOptionKey,
            Function<E, String> labeler,
            Consumer<E> onChange) {

        ComboBox<E> combo = new ComboBox<>();
        combo.setLabel(I18n.t(labelKey));
        if (allOptionKey != null) combo.setPlaceholder(I18n.t(allOptionKey));
        if (labeler != null) combo.setItemLabelGenerator(labeler::apply);
        combo.setClearButtonVisible(true);
        combo.addClassName("filter-select");

        String key = "object:" + labelKey;
        combo.addValueChangeListener(e -> {
            E v = e.getValue();
            if (onChange != null) onChange.accept(v);
            String display = v == null
                    ? null
                    : (labeler != null ? labeler.apply(v) : String.valueOf(v));
            trackChip(key, I18n.t(labelKey), display, combo::clear);
        });
        registerManualField(combo, combo::clear);
        return combo;
    }

    public ComboBox<StatusFilter> addStatusFilter(
            String labelKeyPrefix,
            Consumer<StatusFilter> onChange) {

        ComboBox<StatusFilter> combo = new ComboBox<>();
        combo.setLabel(I18n.t(labelKeyPrefix));
        combo.setItems(StatusFilter.values());
        combo.setItemLabelGenerator(s ->
                I18n.t(labelKeyPrefix + "." + s.name().toLowerCase()));
        combo.setValue(StatusFilter.ALL);
        combo.setClearButtonVisible(false);
        combo.addClassName("filter-select");

        String key = "status:" + labelKeyPrefix;
        combo.addValueChangeListener(e -> {
            StatusFilter v = e.getValue() == null ? StatusFilter.ALL : e.getValue();
            if (onChange != null) onChange.accept(v);
            String display = v == StatusFilter.ALL ? null
                    : I18n.t(labelKeyPrefix + "." + v.name().toLowerCase());
            trackChip(key, I18n.t(labelKeyPrefix), display,
                    () -> combo.setValue(StatusFilter.ALL));
        });
        registerManualField(combo, () -> combo.setValue(StatusFilter.ALL));
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
        insertBefore(control, applyButton);
        if (onClearAction != null) manualClearers.add(onClearAction);
        setVisible(true);
        return control;
    }

    // ══════════════════════════════════════════════════════════════
    //  Layout construction
    // ══════════════════════════════════════════════════════════════

    private void build() {
        addClassName("filter-card");
        setWidthFull();

        // ── Collapse pill ──────────────────────────────────────────
        collapseButton.addClassName("filter-card__collapse");
        collapseButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        collapseButton.setIcon(VaadinIcon.CHEVRON_UP.create());
        collapseButton.getElement().setAttribute("aria-label",
                I18n.t("common.filter.toggle"));
        collapseButton.addClickListener(e -> toggleCollapsed());
        add(collapseButton);

        // ── Header ─────────────────────────────────────────────────
        header.addClassName("filter-card__header");
        Icon filterIcon = VaadinIcon.FILTER.create();
        filterIcon.addClassName("filter-card__icon");
        Span titleSpan = new Span(I18n.t("common.filter.title"));
        titleSpan.addClassName("filter-card__title");
        activeCount.addClassName("filter-card__count");
        header.add(filterIcon, titleSpan, activeCount);
        add(header);

        // ── Body ───────────────────────────────────────────────────
        body.addClassName("filter-card__body");
        controls.addClassName("filter-card__controls");

        // Metadata-driven inputs
        for (var entry : criteriaMetadata.entrySet()) {
            Component input = createInput(entry.getKey(), entry.getValue());
            fieldInputs.put(entry.getKey(), input);
            controls.add(input);
        }

        // Apply / Clear
        applyButton.setText(I18n.t("common.filter.apply"));
        applyButton.setIcon(VaadinIcon.FILTER.create());
        applyButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        applyButton.addClickListener(e -> fireApply());

        clearButton.setText(I18n.t("common.filter.clear"));
        clearButton.setIcon(VaadinIcon.ERASER.create());
        clearButton.addClickListener(e -> fireClear());

        controls.add(applyButton, clearButton);

        // Chips row
        chipsRow.addClassName("filter-card__chips");
        Span chipsLabel = new Span(I18n.t("common.filter.active"));
        chipsLabel.addClassName("filter-card__chips-label");
        chipsList.addClassName("filter-card__chips-list");
        clearAllLink.addClassName("filter-card__clear");
        clearAllLink.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        clearAllLink.setText(I18n.t("common.filter.clear.all"));
        clearAllLink.addClickListener(e -> fireClear());
        chipsRow.add(chipsLabel, chipsList, clearAllLink);

        body.add(controls, chipsRow);
        add(body);

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

    private void insertBefore(Component c, Component anchor) {
        int idx = controls.indexOf(anchor);
        controls.addComponentAtIndex(idx < 0 ? controls.getComponentCount() : idx, c);
    }

    private void registerManualField(Component field, Runnable onClear) {
        if (onClear != null) manualClearers.add(onClear);
        setVisible(true);
    }

    // ══════════════════════════════════════════════════════════════
    //  Metadata-mode input factory — dynamic dispatch by type name
    // ══════════════════════════════════════════════════════════════

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Component createInput(String name, String type) {
        String label = resolveLabel(name);
        String t = type == null ? "String" : type;
        String key = "meta:" + name;

        // ── Enum: match by simple name against the registry ────────
        Class<? extends Enum<?>> enumType = enumRegistry.get(t);
        if (enumType != null) {
            ComboBox combo = new ComboBox();
            combo.setLabel(label);
            combo.setItems(enumType.getEnumConstants());
            combo.setItemLabelGenerator(v -> labelForEnum(name, (Enum<?>) v));
            combo.setClearButtonVisible(true);
            combo.addClassName("filter-select");
            combo.setWidth("12em");
            combo.addValueChangeListener(e -> {
                Enum<?> v = (Enum<?>) e.getValue();
                trackChip(key, label,
                        v == null ? null : labelForEnum(name, v),
                        combo::clear);
            });
            return combo;
        }

        // ── Everything else: switch on the type name ───────────────
        return switch (t) {
            case "Integer", "int" -> {
                IntegerField f = new IntegerField(label);
                f.setClearButtonVisible(true);
                f.setStepButtonsVisible(true);
                f.addClassName("filter-select");
                f.setWidth("10em");
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
                f.setWidth("10em");
                f.addValueChangeListener(e ->
                        trackChip(key, label,
                                e.getValue() == null ? null : e.getValue().toString(),
                                f::clear));
                yield f;
            }
            case "Boolean", "boolean" -> {
                ComboBox<Boolean> f = new ComboBox<>(label);
                f.setItems(Boolean.TRUE, Boolean.FALSE);
                f.setItemLabelGenerator(b -> Boolean.TRUE.equals(b) ? "✓" : "✗");
                f.setClearButtonVisible(true);
                f.addClassName("filter-select");
                f.setWidth("8em");
                f.addValueChangeListener(e ->
                        trackChip(key, label,
                                Boolean.TRUE.equals(e.getValue()) ? "✓" : null,
                                f::clear));
                yield f;
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
                f.setWidth("15em");
                f.addValueChangeListener(e ->
                        trackChip(key, label,
                                e.getValue() == null ? null
                                        : e.getValue().toLocalDate().format(DATE_FMT),
                                () -> f.setValue(null)));
                yield f;
            }
            default -> {
                TextField f = new TextField(label);
                f.setClearButtonVisible(true);
                f.addClassName("filter-select");
                f.setWidth("14em");
                f.addValueChangeListener(e ->
                        trackChip(key, label, e.getValue(), f::clear));
                yield f;
            }
        };
    }

    /**
     * Display label for one enum value.
     * <ol>
     *   <li>i18n key {@code filter.field.<field>.<value_lowercase>}</li>
     *   <li>If the enum implements {@link IEnum}, its {@code meaning()}.</li>
     *   <li>Otherwise {@link Enum#name()}.</li>
     * </ol>
     */
    private String labelForEnum(String fieldName, Enum<?> value) {
        String i18nKey = "filter.field." + fieldName + "." + value.name().toLowerCase();
        String translated = I18n.t(i18nKey);
        if (!translated.equals(i18nKey)) return translated;

        if (value instanceof IEnum ienum) {
            return ienum.meaning();
        }
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

    private static String formatRange(LocalDate from, LocalDate to) {
        if (from == null && to == null) return null;
        if (from != null && to != null)
            return from.format(DATE_FMT) + " → " + to.format(DATE_FMT);
        if (from != null) return "≥ " + from.format(DATE_FMT);
        return "≤ " + to.format(DATE_FMT);
    }

    // ══════════════════════════════════════════════════════════════
    //  Apply / Clear
    // ══════════════════════════════════════════════════════════════

    private void fireApply() {
        if (onApply != null) onApply.accept(buildCriteriaString());
    }

    private void fireClear() {
        fieldInputs.values().forEach(this::clearInput);
        manualClearers.forEach(Runnable::run);
        chips.clear();
        refreshChips();
        if (onClear != null) onClear.run();
    }

    private void clearInput(Component c) {
        if (c instanceof DateRangeFilter drf)   drf.clear();
        else if (c instanceof TextField tf)     tf.clear();
        else if (c instanceof IntegerField f)   f.clear();
        else if (c instanceof NumberField f)    f.clear();
        else if (c instanceof DatePicker f)     f.clear();
        else if (c instanceof DateTimePicker f) f.setValue(null);
        else if (c instanceof ComboBox<?> f)    f.clear();
    }

    // ══════════════════════════════════════════════════════════════
    //  Criteria string (metadata mode)
    // ══════════════════════════════════════════════════════════════

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

    private String buildClause(String field, String type, String raw) {
        String op;
        String value;

        String[] prefixes = {">=", "<=", "!=", ">", "<"};
        String matched = null;
        for (String p : prefixes) {
            if (raw.startsWith(p)) { matched = p; break; }
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
        if (c instanceof TextField tf)          return tf.getValue();
        if (c instanceof IntegerField f)        return f.getValue();
        if (c instanceof NumberField f)         return f.getValue();
        if (c instanceof DatePicker f)          return f.getValue();
        if (c instanceof DateTimePicker f)      return f.getValue();
        if (c instanceof ComboBox<?> f)         return f.getValue();
        return null;
    }
}
package eu.isygoit.ui.common.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import eu.isygoit.i18n.I18n;

import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * Shared renderer that presents every enum value as a coloured tag (chip) in
 * cards, grids and read-only detail surfaces. One look for all modules
 * ({@code wams-enum-tag*} classes, see {@code components/_enum-tag.scss}).
 *
 * <p>Each module keeps a thin static facade (for example {@code KmsEnumTag}) that
 * owns one instance configured with its own fallback translation prefix and
 * "unknown" label key, so existing translations are preserved.
 */
public class EnumTags {

    /** Tone (success / warning / danger / info) of every known enum constant; neutral otherwise. */
    private static final Map<String, String> TONES = Map.ofEntries(
            Map.entry("CANCELLED", "danger"),
            Map.entry("CRITICAL", "danger"),
            Map.entry("DELETED", "danger"),
            Map.entry("DISABLED", "danger"),
            Map.entry("DROPPED_OUT", "danger"),
            Map.entry("EXPELLED", "danger"),
            Map.entry("FAILED", "danger"),
            Map.entry("FAILURE", "danger"),
            Map.entry("HIGH", "danger"),
            Map.entry("INACTIVE", "danger"),
            Map.entry("NON_COMPLIANT", "danger"),
            Map.entry("OVERDUE", "danger"),
            Map.entry("REJECTED", "danger"),
            Map.entry("RESIGNED", "danger"),
            Map.entry("SUSPENDED", "danger"),
            Map.entry("TERMINATED", "danger"),
            Map.entry("DRAFT", "info"),
            Map.entry("EN", "info"),
            Map.entry("MATERNITY_LEAVE", "info"),
            Map.entry("PATERNITY_LEAVE", "info"),
            Map.entry("PRE_ENROLLED", "info"),
            Map.entry("SICK_LEAVE", "info"),
            Map.entry("TRAINING", "info"),
            Map.entry("UNPAID_LEAVE", "info"),
            Map.entry("ACTIVE", "success"),
            Map.entry("APPROVED", "success"),
            Map.entry("AVAILABLE", "success"),
            Map.entry("COMPLETED", "success"),
            Map.entry("COMPLIANT", "success"),
            Map.entry("CONNECTED", "success"),
            Map.entry("ENABLED", "success"),
            Map.entry("FR", "success"),
            Map.entry("GRADUATED", "success"),
            Map.entry("LOW", "success"),
            Map.entry("PAID", "success"),
            Map.entry("PUBLISHED", "success"),
            Map.entry("RESOLVED", "success"),
            Map.entry("SCHEDULED", "success"),
            Map.entry("SENT", "success"),
            Map.entry("SUCCESS", "success"),
            Map.entry("AR", "warning"),
            Map.entry("IN_PROGRESS", "warning"),
            Map.entry("IN_REVIEW", "warning"),
            Map.entry("IN_USE", "warning"),
            Map.entry("MAINTENANCE", "warning"),
            Map.entry("MEDIUM", "warning"),
            Map.entry("ON_LEAVE", "warning"),
            Map.entry("OPEN", "warning"),
            Map.entry("PENDING", "warning"),
            Map.entry("PLANNED", "warning"),
            Map.entry("POSTPONED", "warning"),
            Map.entry("PROBATION", "warning"),
            Map.entry("PROBATIONARY", "warning"),
            Map.entry("UNDER_REVIEW", "warning"),
            Map.entry("WARNING", "warning")
    );

    private static final String[] ALL_TONES = {"success", "warning", "danger", "info", "neutral"};

    private final String fallbackPrefix;
    private final String unknownLabelKey;

    /**
     * @param fallbackPrefix  prefix of the module-wide enum translations ({@code "kms.enum"} gives
     *                        {@code kms.enum.NAME}); may be {@code null} to use the global {@code enum.NAME}
     * @param unknownLabelKey i18n key of the label shown for a missing value; {@code null} shows a dash
     */
    public EnumTags(String fallbackPrefix, String unknownLabelKey) {
        this.fallbackPrefix = fallbackPrefix == null || fallbackPrefix.isBlank() ? "enum" : fallbackPrefix;
        this.unknownLabelKey = unknownLabelKey;
    }

    /* ---- tags ---- */

    public Span of(Enum<?> value) {
        return of(value, null);
    }

    public Span of(Enum<?> value, String translationPrefix) {
        return value == null ? null : ofLabel(label(value, translationPrefix), value.name());
    }

    public Span ofOrUnknown(Enum<?> value, String translationPrefix) {
        return value == null ? ofLabel(unknownLabel(), "UNKNOWN") : of(value, translationPrefix);
    }

    public Span ofValue(String value, String translationPrefix) {
        if (value == null || value.isBlank()) {
            return ofLabel(unknownLabel(), "UNKNOWN");
        }
        return ofLabel(label(value, translationPrefix), value);
    }

    public Span ofLabel(String label, String enumValue) {
        String safeLabel = label == null || label.isBlank() ? unknownLabel() : label;
        Span tag = new Span(safeLabel);
        tag.addClassName("wams-enum-tag");
        tag.addClassName("wams-enum-tag--" + tone(enumValue));
        tag.getElement().setAttribute("title", safeLabel);
        tag.getElement().setAttribute("aria-label", safeLabel);
        return tag;
    }

    /** Refreshes an existing tag in place (text and tone). */
    public void update(Span tag, Enum<?> value, String translationPrefix) {
        refresh(tag, label(value, translationPrefix), value == null ? null : value.name());
    }

    public void update(Span tag, String value, String translationPrefix) {
        refresh(tag, label(value, translationPrefix), value);
    }

    private void refresh(Span tag, String label, String enumValue) {
        tag.setText(label);
        tag.getElement().setAttribute("title", label);
        tag.getElement().setAttribute("aria-label", label);
        tag.addClassName("wams-enum-tag");
        for (String candidate : ALL_TONES) {
            tag.removeClassName("wams-enum-tag--" + candidate);
        }
        tag.addClassName("wams-enum-tag--" + tone(enumValue));
    }

    /* ---- labels ---- */

    public String label(Enum<?> value, String translationPrefix) {
        return value == null ? unknownLabel() : label(value.name(), translationPrefix);
    }

    public String label(String value, String translationPrefix) {
        if (value == null || value.isBlank()) {
            return unknownLabel();
        }
        String upper = value.toUpperCase(Locale.ROOT);
        String lower = value.toLowerCase(Locale.ROOT);
        if (translationPrefix != null && !translationPrefix.isBlank()) {
            for (String candidate : new String[]{value, upper, lower}) {
                String key = translationPrefix + "." + candidate;
                if (I18n.hasKey(key)) {
                    return I18n.t(key);
                }
            }
        }
        for (String candidate : new String[]{value, upper}) {
            String key = fallbackPrefix + "." + candidate;
            if (I18n.hasKey(key)) {
                return I18n.t(key);
            }
        }
        return humanize(value);
    }

    private String unknownLabel() {
        return unknownLabelKey == null ? "—" : I18n.t(unknownLabelKey);
    }

    private static String tone(String enumValue) {
        return enumValue == null ? "neutral" : TONES.getOrDefault(enumValue.toUpperCase(Locale.ROOT), "neutral");
    }

    private static String humanize(String value) {
        String label = value.replace('_', ' ').toLowerCase(Locale.ROOT);
        return Character.toUpperCase(label.charAt(0)) + label.substring(1);
    }

    /* ---- renderers (grid columns, combo items) ---- */

    public <E extends Enum<E>> ComponentRenderer<Span, E> renderer(String translationPrefix) {
        return new ComponentRenderer<>(value -> of(value, translationPrefix));
    }

    public <E extends Enum<E>> void useTagRenderer(ComboBox<E> comboBox, String translationPrefix) {
        comboBox.setRenderer(renderer(translationPrefix));
    }

    public <T, E extends Enum<E>> ComponentRenderer<HorizontalLayout, T> compositeRenderer(
            Function<T, String> primaryLabel,
            Function<T, E> enumValue,
            String translationPrefix) {
        return new ComponentRenderer<>(item -> {
            HorizontalLayout row = new HorizontalLayout();
            row.setAlignItems(FlexComponent.Alignment.CENTER);
            row.setSpacing(true);
            String label = primaryLabel.apply(item);
            row.add(new Span(label == null ? "—" : label),
                    ofOrUnknown(enumValue.apply(item), translationPrefix));
            return row;
        });
    }

    /* ---- read-only detail fields ---- */

    /** Detail-grid field with the same structure as the shared read-only fields, holding any component (a tag). */
    public Component detailField(VaadinIcon icon, String label, Component value) {
        if (value == null) {
            return new Span();
        }
        VerticalLayout field = new VerticalLayout();
        field.setPadding(false);
        field.setSpacing(false);
        field.addClassName("wams-card__detail-field");

        HorizontalLayout labelRow = new HorizontalLayout();
        labelRow.setAlignItems(FlexComponent.Alignment.CENTER);
        labelRow.setSpacing(false);
        labelRow.addClassName("wams-card__detail-field-label-row");

        var iconComponent = icon.create();
        iconComponent.setSize("12px");
        iconComponent.addClassName("detail-field-icon");

        Span labelSpan = new Span(label);
        labelSpan.addClassName("wams-card__detail-field-label");
        labelRow.add(iconComponent, labelSpan);

        value.addClassName("wams-card__detail-field-value");
        field.add(labelRow, value);
        return field;
    }

    public Component detailField(VaadinIcon icon, String label, Enum<?> value, String translationPrefix) {
        return value == null ? null : detailField(icon, label, of(value, translationPrefix));
    }

    /** Adds an enum detail field to the grid; a missing value is shown as an "unknown" tag. */
    public void addDetailField(Div container, VaadinIcon icon, String label, Enum<?> value, String translationPrefix) {
        container.add(detailField(icon, label, ofOrUnknown(value, translationPrefix)));
    }

    public void addTagDetailField(Div container, VaadinIcon icon, String label, Component tag) {
        if (tag != null) {
            container.add(detailField(icon, label, tag));
        }
    }
}
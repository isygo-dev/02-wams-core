package eu.isygoit.ui.mms.views.common;

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

public final class MmsEnumTag {

    private static final String[] TONES = {"success", "warning", "danger", "info", "neutral"};
    private static final Map<String, String> ENUM_TONES = Map.ofEntries(
            Map.entry("ACTIVE", "success"),
            Map.entry("APPROVED", "success"),
            Map.entry("AVAILABLE", "success"),
            Map.entry("COMPLETED", "success"),
            Map.entry("COMPLIANT", "success"),
            Map.entry("CONNECTED", "success"),
            Map.entry("ENABLED", "success"),
            Map.entry("GRADUATED", "success"),
            Map.entry("LOW", "success"),
            Map.entry("PAID", "success"),
            Map.entry("PUBLISHED", "success"),
            Map.entry("RESOLVED", "success"),
            Map.entry("SCHEDULED", "success"),
            Map.entry("SENT", "success"),
            Map.entry("SUCCESS", "success"),
            Map.entry("CANCELLED", "danger"),
            Map.entry("CRITICAL", "danger"),
            Map.entry("DELETED", "danger"),
            Map.entry("DISABLED", "danger"),
            Map.entry("DROPPED_OUT", "danger"),
            Map.entry("EXPELLED", "danger"),
            Map.entry("FAILURE", "danger"),
            Map.entry("FAILED", "danger"),
            Map.entry("HIGH", "danger"),
            Map.entry("INACTIVE", "danger"),
            Map.entry("NON_COMPLIANT", "danger"),
            Map.entry("OVERDUE", "danger"),
            Map.entry("REJECTED", "danger"),
            Map.entry("RESIGNED", "danger"),
            Map.entry("SUSPENDED", "danger"),
            Map.entry("TERMINATED", "danger"),
            Map.entry("WARNING", "warning"),
            Map.entry("IN_PROGRESS", "warning"),
            Map.entry("IN_REVIEW", "warning"),
            Map.entry("IN_USE", "warning"),
            Map.entry("MAINTENANCE", "warning"),
            Map.entry("MEDIUM", "warning"),
            Map.entry("OPEN", "warning"),
            Map.entry("ON_LEAVE", "warning"),
            Map.entry("PENDING", "warning"),
            Map.entry("PLANNED", "warning"),
            Map.entry("POSTPONED", "warning"),
            Map.entry("PROBATION", "warning"),
            Map.entry("PROBATIONARY", "warning"),
            Map.entry("UNDER_REVIEW", "warning"),
            Map.entry("DRAFT", "info"),
            Map.entry("PRE_ENROLLED", "info"),
            Map.entry("TRAINING", "info"),
            Map.entry("UNPAID_LEAVE", "info"),
            Map.entry("SICK_LEAVE", "info"),
            Map.entry("MATERNITY_LEAVE", "info"),
            Map.entry("PATERNITY_LEAVE", "info"),
            Map.entry("EN", "info"),
            Map.entry("FR", "success"),
            Map.entry("AR", "warning")
    );

    private MmsEnumTag() {
    }

    public static Span of(Enum<?> value, String translationPrefix) {
        return ofValue(value == null ? null : value.name(), translationPrefix);
    }

    public static Span ofOrUnknown(Enum<?> value, String translationPrefix) {
        return value == null
                ? ofLabel(I18n.t("mms.common.value.notAvailable"), "UNKNOWN")
                : of(value, translationPrefix);
    }

    public static Span ofValue(String value, String translationPrefix) {
        String safeValue = value == null || value.isBlank() ? null : value;
        String label = safeValue == null
                ? I18n.t("mms.common.value.notAvailable")
                : label(safeValue, translationPrefix);
        return ofLabel(label, safeValue == null ? "UNKNOWN" : safeValue);
    }

    public static String label(Enum<?> value, String translationPrefix) {
        return value == null
                ? I18n.t("mms.common.value.notAvailable")
                : label(value.name(), translationPrefix);
    }

    public static String label(String value, String translationPrefix) {
        if (value == null || value.isBlank()) {
            return I18n.t("mms.common.value.notAvailable");
        }
        if (translationPrefix != null && !translationPrefix.isBlank()) {
            String key = translationPrefix + "." + value;
            if (I18n.hasKey(key)) {
                return I18n.t(key);
            }
            key = translationPrefix + "." + value.toLowerCase(Locale.ROOT);
            if (I18n.hasKey(key)) {
                return I18n.t(key);
            }
        }
        return humanize(value);
    }

    public static void update(Span tag, Enum<?> value, String translationPrefix) {
        update(tag, value == null ? null : value.name(), translationPrefix);
    }

    public static void update(Span tag, String value, String translationPrefix) {
        Span updated = ofValue(value, translationPrefix);
        tag.setText(updated.getText());
        tag.addClassName("mms-enum-tag");
        for (String tone : TONES) {
            tag.removeClassName("mms-enum-tag--" + tone);
        }
        tag.addClassName("mms-enum-tag--" + tone(value));
        tag.getElement().setAttribute("title", updated.getText());
        tag.getElement().setAttribute("aria-label", updated.getText());
    }

    public static Span ofLabel(String label, String enumValue) {
        String safeLabel = label == null || label.isBlank()
                ? I18n.t("mms.common.value.notAvailable") : label;
        Span tag = new Span(safeLabel);
        tag.addClassName("mms-enum-tag");
        tag.addClassName("mms-enum-tag--" + tone(enumValue));
        tag.getElement().setAttribute("title", safeLabel);
        tag.getElement().setAttribute("aria-label", safeLabel);
        return tag;
    }

    public static <E extends Enum<E>> ComponentRenderer<Span, E> renderer(String translationPrefix) {
        return new ComponentRenderer<>(value -> ofOrUnknown(value, translationPrefix));
    }

    public static <E extends Enum<E>> void useTagRenderer(
            ComboBox<E> comboBox,
            String translationPrefix) {
        comboBox.setRenderer(renderer(translationPrefix));
        comboBox.setItemLabelGenerator(value -> label(value, translationPrefix));
    }

    public static Component detailField(VaadinIcon icon, String label, Component value) {
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

    public static void addDetailField(
            Div container,
            VaadinIcon icon,
            String label,
            Enum<?> value,
            String translationPrefix) {
        container.add(detailField(icon, label, ofOrUnknown(value, translationPrefix)));
    }

    private static String tone(String value) {
        return value == null
                ? "neutral"
                : ENUM_TONES.getOrDefault(value.toUpperCase(Locale.ROOT), "neutral");
    }

    private static String humanize(String value) {
        String label = value.replace('_', ' ').toLowerCase(Locale.ROOT);
        return Character.toUpperCase(label.charAt(0)) + label.substring(1);
    }
}

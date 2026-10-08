package eu.isygoit.ui.ims.views.common;

import com.vaadin.flow.component.Component;
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

public final class ImsEnumTag {

    private static final Map<String, String> TONES = Map.ofEntries(
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
            Map.entry("PATERNITY_LEAVE", "info")
    );

    private ImsEnumTag() {
    }

    public static Span of(Enum<?> value) {
        return of(value, null);
    }

    public static <E extends Enum<E>> ComponentRenderer<Span, E> renderer(String translationPrefix) {
        return new ComponentRenderer<>(value -> of(value, translationPrefix));
    }

    public static Span of(Enum<?> value, String translationPrefix) {
        return value == null ? null : ofLabel(label(value, translationPrefix), value.name());
    }

    public static Span ofOrUnknown(Enum<?> value, String translationPrefix) {
        return value == null
                ? ofLabel(I18n.t("ims.enum.unknown"), "UNKNOWN")
                : of(value, translationPrefix);
    }

    public static Span ofValue(String value, String translationPrefix) {
        if (value == null || value.isBlank()) {
            return ofLabel(I18n.t("ims.enum.unknown"), "UNKNOWN");
        }
        String enumKey = "ims.enum." + value.toUpperCase(Locale.ROOT);
        String key = translationPrefix == null || translationPrefix.isBlank()
                ? enumKey
                : translationPrefix + "." + value.toUpperCase(Locale.ROOT);
        if (translationPrefix != null && !translationPrefix.isBlank() && !I18n.hasKey(key)) {
            key = translationPrefix + "." + value.toLowerCase(Locale.ROOT);
        }
        if (!I18n.hasKey(key)) {
            key = enumKey;
        }
        String label = I18n.hasKey(key) ? I18n.t(key) : humanize(value);
        return ofLabel(label, value);
    }

    public static void update(Span tag, Enum<?> value, String translationPrefix) {
        String label = value == null
                ? I18n.t("ims.enum.unknown")
                : label(value, translationPrefix);
        String tone = value == null
                ? "neutral"
                : TONES.getOrDefault(value.name().toUpperCase(Locale.ROOT), "neutral");
        tag.setText(label);
        tag.getElement().setAttribute("title", label);
        tag.getElement().setAttribute("aria-label", label);
        for (String candidate : new String[]{"success", "warning", "danger", "info", "neutral"}) {
            tag.removeClassName("ims-enum-tag--" + candidate);
        }
        tag.addClassName("ims-enum-tag--" + tone);
    }

    public static String label(Enum<?> value, String translationPrefix) {
        if (value == null) {
            return I18n.t("ims.enum.unknown");
        }

        if (translationPrefix != null && !translationPrefix.isBlank()) {
            String upperKey = translationPrefix + "." + value.name();
            String lowerKey = translationPrefix + "." + value.name().toLowerCase(Locale.ROOT);
            if (I18n.hasKey(upperKey)) {
                return I18n.t(upperKey);
            }
            if (I18n.hasKey(lowerKey)) {
                return I18n.t(lowerKey);
            }
        }

        String key = "ims.enum." + value.name();
        if (I18n.hasKey(key)) {
            return I18n.t(key);
        }

        return humanize(value.name());
    }

    private static String humanize(String rawValue) {
        String humanized = rawValue.replace('_', ' ').toLowerCase(Locale.ROOT);
        return Character.toUpperCase(humanized.charAt(0)) + humanized.substring(1);
    }

    public static Span ofLabel(String label, String enumValue) {
        String safeLabel = label == null || label.isBlank() ? I18n.t("ims.enum.unknown") : label;
        String tone = enumValue == null
                ? "neutral"
                : TONES.getOrDefault(enumValue.toUpperCase(Locale.ROOT), "neutral");

        Span tag = new Span(safeLabel);
        tag.addClassName("ims-enum-tag");
        tag.addClassName("ims-enum-tag--" + tone);
        tag.getElement().setAttribute("title", safeLabel);
        tag.getElement().setAttribute("aria-label", safeLabel);
        return tag;
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
        Span tag = value == null
                ? ofLabel(I18n.t("ims.enum.unknown"), "UNKNOWN")
                : of(value, translationPrefix);
        container.add(detailField(icon, label, tag));
    }
}

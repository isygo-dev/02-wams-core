package eu.isygoit.ui.mms.views.common;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import eu.isygoit.ui.common.component.EnumTags;

import java.util.function.Function;

/**
 * MMS entry point to the shared enum tag renderer ({@link EnumTags}): every enum is
 * presented as a tag. Only the translation fallbacks are module specific.
 */
public final class MmsEnumTag {

    private static final EnumTags TAGS = new EnumTags(null, "mms.common.value.notAvailable");

    private MmsEnumTag() {
    }

    public static Span of(Enum<?> value) {
        return TAGS.of(value);
    }

    public static Span of(Enum<?> value, String translationPrefix) {
        return TAGS.of(value, translationPrefix);
    }

    public static Span ofOrUnknown(Enum<?> value, String translationPrefix) {
        return TAGS.ofOrUnknown(value, translationPrefix);
    }

    public static Span ofValue(String value, String translationPrefix) {
        return TAGS.ofValue(value, translationPrefix);
    }

    public static Span ofLabel(String label, String enumValue) {
        return TAGS.ofLabel(label, enumValue);
    }

    public static String label(Enum<?> value, String translationPrefix) {
        return TAGS.label(value, translationPrefix);
    }

    public static String label(String value, String translationPrefix) {
        return TAGS.label(value, translationPrefix);
    }

    public static void update(Span tag, Enum<?> value, String translationPrefix) {
        TAGS.update(tag, value, translationPrefix);
    }

    public static void update(Span tag, String value, String translationPrefix) {
        TAGS.update(tag, value, translationPrefix);
    }

    public static <E extends Enum<E>> ComponentRenderer<Span, E> renderer(String translationPrefix) {
        return TAGS.renderer(translationPrefix);
    }

    public static <E extends Enum<E>> void useTagRenderer(ComboBox<E> comboBox, String translationPrefix) {
        TAGS.useTagRenderer(comboBox, translationPrefix);
    }

    public static <T, E extends Enum<E>> ComponentRenderer<HorizontalLayout, T> compositeRenderer(
            Function<T, String> primaryLabel,
            Function<T, E> enumValue,
            String translationPrefix) {
        return TAGS.compositeRenderer(primaryLabel, enumValue, translationPrefix);
    }

    public static Component detailField(VaadinIcon icon, String label, Component value) {
        return TAGS.detailField(icon, label, value);
    }

    public static Component detailField(VaadinIcon icon, String label, Enum<?> value, String translationPrefix) {
        return TAGS.detailField(icon, label, value, translationPrefix);
    }

    public static void addDetailField(Div container, VaadinIcon icon, String label,
                                      Enum<?> value, String translationPrefix) {
        TAGS.addDetailField(container, icon, label, value, translationPrefix);
    }

    public static void addTagDetailField(Div container, VaadinIcon icon, String label, Component tag) {
        TAGS.addTagDetailField(container, icon, label, tag);
    }
}
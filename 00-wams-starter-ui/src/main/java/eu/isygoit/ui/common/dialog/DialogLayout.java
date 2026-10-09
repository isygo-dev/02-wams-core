package eu.isygoit.ui.common.dialog;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * Shared layout vocabulary for form and details dialogs of every module: width
 * scale, responsive form and titled sections. Styles live in
 * {@code styles/scss/components/_dialog-layout.scss}; modules must not restyle them.
 */
public final class DialogLayout {

    /** Compact dialogs: a handful of fields, no grouping. */
    public static final String WIDTH_S = "30rem";
    /** Standard dialogs: one logical group, up to ~8 fields. */
    public static final String WIDTH_M = "40rem";
    /** Wide dialogs: several logical groups (tabbed). */
    public static final String WIDTH_L = "56rem";
    /** Keeps every dialog inside the viewport on small screens. */
    public static final String MAX_WIDTH = "95%";

    /** Viewport width from which forms switch from one to two columns. */
    public static final String TWO_COLUMNS_FROM = "37.5rem";

    public static final String CLASS_STACK = "wams-dlg-stack";
    public static final String CLASS_LIST = "wams-dlg-list";
    public static final String CLASS_HERO = "wams-dlg-hero";
    public static final String CLASS_HERO_INFO = "wams-dlg-hero__info";
    public static final String CLASS_HERO_TITLE = "wams-dlg-hero__title";
    public static final String CLASS_HERO_NAME = "wams-dlg-hero__name";
    public static final String CLASS_HERO_CHIPS = "wams-dlg-hero__chips";
    public static final String CLASS_HERO_SUBTITLE = "wams-dlg-hero__subtitle";
    public static final String CLASS_AVATAR = "wams-dlg-avatar";
    public static final String CLASS_AVATAR_ICON = "wams-dlg-avatar__icon";
    public static final String CLASS_AVATAR_IMG = "wams-dlg-avatar__img";
    public static final String CLASS_HELP = "wams-dlg-help";
    public static final String CLASS_TEXTAREA = "wams-dlg-textarea";
    public static final String CLASS_EMPTY = "wams-dlg-empty";
    public static final String CLASS_EMPTY_BOXED = "wams-dlg-empty--boxed";
    public static final String CLASS_CARD = "wams-dlg-card";
    public static final String CLASS_ROW = "wams-dlg-row";
    public static final String CLASS_NOTE = "wams-dlg-note";
    public static final String CLASS_CHIP = "wams-dlg-chip";

    private static final String CLASS_SECTION = "wams-dlg-section";
    private static final String CLASS_SECTION_TITLE = "wams-dlg-section__title";
    private static final String CLASS_SECTION_ICON = "wams-dlg-section__icon";

    private DialogLayout() {
    }

    /** Applies the shared width scale and keeps the dialog usable on small screens. */
    public static void size(Dialog dialog, String width) {
        dialog.setWidth(width);
        dialog.setMaxWidth(MAX_WIDTH);
        dialog.setResizable(true);
    }

    /** One column on mobile, two columns from {@link #TWO_COLUMNS_FROM}. */
    public static FormLayout responsiveForm() {
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep(TWO_COLUMNS_FROM, 2));
        return form;
    }

    /** Vertical stack with the shared dialog rhythm. */
    public static VerticalLayout stack() {
        VerticalLayout stack = new VerticalLayout();
        stack.setPadding(false);
        stack.setSpacing(true);
        stack.setWidthFull();
        stack.addClassName(CLASS_STACK);
        return stack;
    }

    /** Titled section card (icon + title); the caller adds the content. */
    public static VerticalLayout section(String title, VaadinIcon icon) {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(false);
        section.setSpacing(true);
        section.setWidthFull();
        section.addClassName(CLASS_SECTION);

        Icon sectionIcon = icon.create();
        sectionIcon.addClassName(CLASS_SECTION_ICON);

        Span heading = new Span(sectionIcon, new Span(" " + title));
        heading.addClassName(CLASS_SECTION_TITLE);
        section.add(heading);
        return section;
    }

    /** Small secondary text shown under a section title or field group. */
    public static Span help(String text) {
        Span help = new Span(text);
        help.addClassName(CLASS_HELP);
        return help;
    }

    /** Full-width multi-line field with the shared minimum height. */
    public static <T extends Component & HasSize> T tall(T field) {
        field.setWidthFull();
        field.addClassName(CLASS_TEXTAREA);
        return field;
    }
}

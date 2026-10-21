package eu.isygoit.ui.common.component;

import com.vaadin.flow.dom.Element;

/**
 * Forces an element (and, through CSS inheritance, its shadow-DOM content) to render
 * left-to-right regardless of the page's own direction — for values with no strong-direction
 * characters of their own (phone numbers, dates: just digits and separators) that the
 * Unicode Bidi Algorithm would otherwise reorder as a whole inside a right-to-left page.
 * <p>
 * Sets both the {@code dir} attribute (for assistive tech and any CSS keyed off it) and real
 * inline CSS {@code direction}/{@code unicode-bidi}: the attribute alone is a low-priority
 * presentational hint that a component's own {@code direction: inherit} styling (as Vaadin's
 * text fields and date pickers use) can silently override once the page itself is
 * right-to-left, which is exactly what caused a phone number typed as {@code +216...} to
 * show as {@code ...612+} in the Arabic locale.
 */
public final class BidiLtr {

    private static final String LRI = "⁦";
    private static final String PDI = "⁩";

    private BidiLtr() {
    }

    /** Forces left-to-right rendering on an input/editable element. */
    public static void apply(Element element) {
        element.setAttribute("dir", "ltr");
        element.getStyle().set("direction", "ltr");
        element.getStyle().set("unicode-bidi", "isolate");
    }

    /** Undoes {@link #apply(Element)}, for an element reused for something else (e.g. a
     * contact-type picker switching from phone to email). */
    public static void clear(Element element) {
        element.removeAttribute("dir");
        element.getStyle().remove("direction");
        element.getStyle().remove("unicode-bidi");
    }

    /**
     * Wraps a value for safe READ-ONLY plain-text display (a grid cell, a card row, a
     * tooltip) next to right-to-left text, using Unicode bidi-isolate marks — the only option
     * where there is no element of our own to put {@link #apply(Element)} on.
     */
    public static String wrap(String value) {
        return (value == null || value.isBlank()) ? value : LRI + value + PDI;
    }
}

package eu.isygoit.ui.common.component;

import com.vaadin.flow.server.VaadinSession;
import eu.isygoit.i18n.I18n;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Which digits numbers and dates are written with: Latin ({@code 1,600,000}) or Arabic-Indic
 * ({@code ١٬٦٠٠٬٠٠٠}). The institution sets it in its profile; it is kept per user session. Every number and date
 * display builds its formats through this class so the whole screen follows the setting. Latin is the default.
 */
public final class NumberStyle {

    /** Unicode numbering system of Latin digits. */
    public static final String LATIN = "latn";
    /** Unicode numbering system of Arabic-Indic digits. */
    public static final String ARABIC_INDIC = "arab";

    private static final String SESSION_KEY = "wams-numeral-system";

    private NumberStyle() {
    }

    /** Sets the numbering system of this session; anything other than Arabic-Indic means Latin. */
    public static void use(String system) {
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            session.setAttribute(SESSION_KEY, ARABIC_INDIC.equalsIgnoreCase(system) ? ARABIC_INDIC : LATIN);
        }
    }

    /** The numbering system of this session. */
    public static String system() {
        VaadinSession session = VaadinSession.getCurrent();
        Object stored = session == null ? null : session.getAttribute(SESSION_KEY);
        return ARABIC_INDIC.equals(stored) ? ARABIC_INDIC : LATIN;
    }

    /** The locale with the numbering system of the session forced (e.g. {@code ar_SA} with Latin digits). */
    public static Locale localize(Locale base) {
        Locale locale = base == null ? I18n.getCurrentLocale() : base;
        return new Locale.Builder().setLocale(locale).setUnicodeLocaleKeyword("nu", system()).build();
    }

    /** Number format of the current language, written with the session's digits. */
    public static NumberFormat numberFormat() {
        return NumberFormat.getNumberInstance(localize(I18n.getCurrentLocale()));
    }

    /** Integer format of the current language, written with the session's digits. */
    public static NumberFormat integerFormat() {
        return NumberFormat.getIntegerInstance(localize(I18n.getCurrentLocale()));
    }
}

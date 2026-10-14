package eu.isygoit.ui.common.layout;

import com.vaadin.flow.component.UI;
import eu.isygoit.i18n.I18n;

import java.util.Locale;

/**
 * Applies the page-level settings every screen must share, whichever view or layout is shown first: the text
 * direction and language (Arabic is right-to-left) and the Lumo light/dark theme.
 *
 * <p>It is invoked when a UI starts and again whenever a layout or a login view is shown, so the settings are
 * restored on full page loads, on screens that do not use the main layout (sign-in, landing) and in module
 * applications that run on another port. The theme choice is kept in a cookie (cookies are shared across ports of
 * the same host, local storage is not) and mirrored in local storage.</p>
 */
public final class PageEnvironment {

    public static final String THEME_KEY = "wams-theme";

    private static final String THEME_READ_JS =
            "var s=null;"
                    + "try{var m=document.cookie.match(new RegExp('(?:^|; )" + THEME_KEY + "=([^;]*)'));if(m){s=decodeURIComponent(m[1]);}}catch(e){}"
                    + "if(!s){try{s=window.localStorage.getItem('" + THEME_KEY + "')}catch(e){}}"
                    + "var dark=s?s==='dark':!!(window.matchMedia&&window.matchMedia('(prefers-color-scheme: dark)').matches);"
                    + "document.documentElement.setAttribute('theme',dark?'dark':'light');";

    private static final String THEME_WRITE_JS =
            "document.documentElement.setAttribute('theme',$0?'dark':'light');"
                    + "var v=$0?'dark':'light';"
                    + "try{document.cookie='" + THEME_KEY + "='+v+'; path=/; max-age=31536000; SameSite=Lax'}catch(e){}"
                    + "try{window.localStorage.setItem('" + THEME_KEY + "',v)}catch(e){}";

    private PageEnvironment() {
    }

    public static boolean isRightToLeft(Locale locale) {
        return locale != null && "ar".equalsIgnoreCase(locale.getLanguage());
    }

    /**
     * "rtl" or "ltr" for the session language.
     */
    public static String currentDirection() {
        return isRightToLeft(I18n.getCurrentLocale()) ? "rtl" : "ltr";
    }

    /**
     * Sets dir/lang on the document and restores the stored (or OS-preferred) theme.
     */
    public static void apply(UI ui) {
        if (ui == null) {
            return;
        }
        Locale locale = I18n.getCurrentLocale();
        String direction = isRightToLeft(locale) ? "rtl" : "ltr";
        ui.getElement().setAttribute("dir", direction);
        ui.getPage().executeJs(
                "document.documentElement.setAttribute('dir',$0);"
                        + "document.documentElement.setAttribute('lang',$1);"
                        + THEME_READ_JS,
                direction, locale != null ? locale.toLanguageTag() : "en");
    }

    /**
     * Applies and remembers a theme choice made with the header toggle.
     */
    public static void storeTheme(UI ui, boolean dark) {
        if (ui != null) {
            ui.getPage().executeJs(THEME_WRITE_JS, dark);
        }
    }
}

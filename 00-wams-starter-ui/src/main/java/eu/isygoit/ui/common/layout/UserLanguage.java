package eu.isygoit.ui.common.layout;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinSession;
import eu.isygoit.i18n.DefaultLanguageProvider;
import eu.isygoit.i18n.I18n;

import jakarta.servlet.http.Cookie;
import java.util.Locale;
import java.util.Optional;

/**
 * Which language a user sees. In order: the language the user picked (in this session, else the one remembered in a
 * cookie by an earlier visit), then the default language of the institution ({@link DefaultLanguageProvider}), then
 * what the browser or the provider gives. The choice is remembered in a cookie like the theme, so it survives new
 * sessions and is shared by the applications of the same host.
 */
public final class UserLanguage {

    /** Name of the cookie holding the language the user picked. */
    public static final String COOKIE = "wams-language";

    private static final String SESSION_LOCALE = "locale";

    private UserLanguage() {
    }

    /** Remembers the language picked by the user, for this session and for the next visits. */
    public static void remember(UI ui, Locale locale) {
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            session.setAttribute(SESSION_LOCALE, locale);
        }
        if (ui != null) {
            ui.getPage().executeJs("try{document.cookie='" + COOKIE
                    + "='+$0+'; path=/; max-age=31536000; SameSite=Lax'}catch(e){}", locale.toLanguageTag());
        }
    }

    /** The language the user picked, if any: this session's choice, else the remembered one. */
    public static Optional<Locale> chosen() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null && session.getAttribute(SESSION_LOCALE) instanceof Locale locale) {
            return Optional.of(locale);
        }
        VaadinRequest request = VaadinRequest.getCurrent();
        Cookie[] cookies = request == null ? null : request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (COOKIE.equals(cookie.getName()) && cookie.getValue() != null) {
                    return supported(Locale.forLanguageTag(cookie.getValue()));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Sets the language of a starting UI: the user's choice, else the institution default; nothing changes when
     * neither exists. Called before the page environment (direction, lang) is applied.
     */
    public static void applyInitial(UI ui, DefaultLanguageProvider institutionDefault) {
        if (institutionDefault != null) {
            institutionDefault.numeralSystem().ifPresent(eu.isygoit.ui.common.component.NumberStyle::use);
        }
        Locale locale = chosen().or(() -> institutionDefault == null
                ? Optional.empty() : institutionDefault.defaultLanguage().flatMap(UserLanguage::supported)).orElse(null);
        if (locale != null && !locale.equals(I18n.getCurrentLocale())) {
            I18n.setLocale(locale);
            if (ui != null) {
                ui.setLocale(locale);
            }
        }
    }

    /** The supported locale of the same language (a bare {@code fr} gives {@code fr_FR}), if any. */
    public static Optional<Locale> supported(Locale wanted) {
        if (wanted == null || wanted.getLanguage().isBlank()) {
            return Optional.empty();
        }
        return I18n.getSupportedLocales().stream()
                .filter(locale -> locale.equals(wanted))
                .findFirst()
                .or(() -> I18n.getSupportedLocales().stream()
                        .filter(locale -> locale.getLanguage().equalsIgnoreCase(wanted.getLanguage()))
                        .findFirst());
    }
}

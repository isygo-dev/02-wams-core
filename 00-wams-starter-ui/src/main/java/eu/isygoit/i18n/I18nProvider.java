package eu.isygoit.i18n;

import com.vaadin.flow.i18n.LocaleChangeEvent;
import com.vaadin.flow.i18n.LocaleChangeObserver;
import com.vaadin.flow.server.VaadinSession;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Provider pour la gestion de l'internationalisation (i18n) dans l'application.
 * Les traductions sont réparties par module fonctionnel (common, auth, ims, kms,
 * dms, mms, sms, cms) sous {@code classpath:i18n/<module>/messages_<locale>.properties},
 * puis fusionnées en mémoire par locale.
 */
@Component
public class I18nProvider implements com.vaadin.flow.i18n.I18NProvider, LocaleChangeObserver {

    private static final String[] MODULES = {
            "common", "auth", "ims", "kms", "dms", "mms", "sms", "cms"
    };

    private static final Locale[] SUPPORTED_LOCALES = {
            new Locale("en", "US"),
            new Locale("fr", "FR"),
            new Locale("es", "ES"),
            new Locale("it", "IT"),
            new Locale("de", "DE"),
            new Locale("ar", "SA")
    };

    private static final Locale DEFAULT_LOCALE = new Locale("en", "US");

    private final Map<Locale, Map<String, String>> translationsByLocale = new HashMap<>();

    public I18nProvider() {
        for (Locale locale : SUPPORTED_LOCALES) {
            translationsByLocale.put(locale, loadMergedBundle(locale));
        }
    }

    /**
     * Charge et fusionne les fichiers de traduction de tous les modules pour une locale donnée.
     */
    private Map<String, String> loadMergedBundle(Locale locale) {
        Map<String, String> merged = new HashMap<>();
        String localeTag = locale.getLanguage() + "_" + locale.getCountry();
        for (String module : MODULES) {
            String path = "i18n/" + module + "/messages_" + localeTag + ".properties";
            Properties properties = new Properties();
            try (InputStream stream = getClass().getClassLoader().getResourceAsStream(path)) {
                if (stream == null) {
                    System.err.println("Missing i18n resource: " + path);
                    continue;
                }
                try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                    properties.load(reader);
                }
            } catch (IOException e) {
                System.err.println("Failed to load i18n resource: " + path + " (" + e.getMessage() + ")");
                continue;
            }
            for (String key : properties.stringPropertyNames()) {
                merged.put(key, properties.getProperty(key));
            }
        }
        return merged;
    }

    @Override
    public List<Locale> getProvidedLocales() {
        return Arrays.asList(SUPPORTED_LOCALES);
    }

    @Override
    public String getTranslation(String key, Locale locale, Object... params) {
        if (key == null) {
            return "";
        }

        Map<String, String> bundle = translationsByLocale.get(locale);
        String message = bundle != null ? bundle.get(key) : null;

        if (message == null) {
            // Fall back to English
            Map<String, String> fallback = translationsByLocale.get(DEFAULT_LOCALE);
            message = fallback != null ? fallback.get(key) : null;
        }

        if (message == null) {
            return "!" + key + "!";
        }

        if (params != null && params.length > 0) {
            return String.format(message, params);
        }
        return message;
    }

    @Override
    public void localeChange(LocaleChangeEvent event) {
        VaadinSession.getCurrent().setLocale(event.getLocale());
    }

    /**
     * Get a translation using the current session locale
     */
    public String get(String key, Object... params) {
        Locale locale = VaadinSession.getCurrent() != null ?
                VaadinSession.getCurrent().getLocale() :
                DEFAULT_LOCALE;
        return getTranslation(key, locale, params);
    }

    /**
     * Get a translation for a specific locale
     */
    public String get(String key, Locale locale, Object... params) {
        return getTranslation(key, locale, params);
    }

    /**
     * Set the current locale
     */
    public void setLocale(Locale locale) {
        if (VaadinSession.getCurrent() != null) {
            VaadinSession.getCurrent().setLocale(locale);
        }
    }

    /**
     * Get the current locale
     */
    public Locale getCurrentLocale() {
        return VaadinSession.getCurrent() != null ?
                VaadinSession.getCurrent().getLocale() :
                DEFAULT_LOCALE;
    }
}

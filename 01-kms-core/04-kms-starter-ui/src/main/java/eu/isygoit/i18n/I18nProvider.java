package eu.isygoit.i18n;

import com.vaadin.flow.i18n.LocaleChangeEvent;
import com.vaadin.flow.i18n.LocaleChangeObserver;
import com.vaadin.flow.server.VaadinSession;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Provider pour la gestion de l'internationalisation (i18n) dans l'application.
 * Gère le chargement et la sélection des fichiers de ressources de traduction.
 */
@Component
public class I18nProvider implements com.vaadin.flow.i18n.I18NProvider, LocaleChangeObserver {

    private static final String BUNDLE_NAME = "messages";
    private static final Locale[] SUPPORTED_LOCALES = {
            new Locale("en", "US"),
            new Locale("fr", "FR"),
            new Locale("es", "ES"),
            new Locale("de", "DE")
    };

    private final Map<Locale, ResourceBundle> bundleCache = new HashMap<>();

    public I18nProvider() {
        // Pre-load all resource bundles
        for (Locale locale : SUPPORTED_LOCALES) {
            try {
                bundleCache.put(locale, ResourceBundle.getBundle(BUNDLE_NAME, locale));
            } catch (MissingResourceException e) {
                System.err.println("Failed to load resource bundle for locale: " + locale);
            }
        }
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

        // Try to get the bundle for the requested locale
        ResourceBundle bundle = bundleCache.get(locale);
        if (bundle == null) {
            // Fall back to English
            bundle = bundleCache.get(new Locale("en", "US"));
        }

        try {
            String message = bundle.getString(key);
            // Format the message with parameters if any
            if (params != null && params.length > 0) {
                return String.format(message, params);
            }
            return message;
        } catch (MissingResourceException e) {
            // Return the key itself if translation not found
            return "!" + key + "!";
        }
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
                new Locale("en", "US");
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
                new Locale("en", "US");
    }
}


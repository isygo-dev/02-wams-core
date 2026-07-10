package eu.isygoit.i18n;

import com.vaadin.flow.i18n.LocaleChangeEvent;
import com.vaadin.flow.i18n.LocaleChangeObserver;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.server.VaadinSession;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Abstract base class for i18n providers that loads translations from multiple module-specific bundles.
 * Subclasses must provide the list of modules and supported locales.
 */
public abstract class AbstractI18nProvider implements I18NProvider, LocaleChangeObserver {

    private final Map<Locale, Map<String, String>> translationsByLocale = new HashMap<>();
    private Locale defaultLocale;

    /**
     * Constructor that initializes translations for all supported locales.
     */
    protected AbstractI18nProvider() {
        this.defaultLocale = getDefaultLocale();
        for (Locale locale : getSupportedLocales()) {
            translationsByLocale.put(locale, loadMergedBundle(locale));
        }
    }

    /**
     * Get the list of module names whose translation files should be loaded.
     *
     * @return array of module names
     */
    protected abstract String[] getModules();

    /**
     * Get the list of supported locales.
     *
     * @return array of supported locales
     */
    protected abstract Locale[] getSupportedLocales();

    /**
     * Get the default locale to use as fallback.
     * Default implementation returns the first supported locale or Locale.US.
     *
     * @return the default locale
     */
    protected Locale getDefaultLocale() {
        Locale[] supported = getSupportedLocales();
        return (supported != null && supported.length > 0) ? supported[0] : Locale.US;
    }

    /**
     * Get the base path for i18n resource files.
     * Override this method to change the resource location.
     *
     * @return the base path (default: "i18n/")
     */
    protected String getBasePath() {
        return "i18n/";
    }

    /**
     * Get the file extension for i18n resource files.
     * Override this method to change the file extension.
     *
     * @return the file extension (default: ".properties")
     */
    protected String getFileExtension() {
        return ".properties";
    }

    /**
     * Loads and merges translation files from all modules for a given locale.
     */
    private Map<String, String> loadMergedBundle(Locale locale) {
        Map<String, String> merged = new HashMap<>();
        String localeTag = locale.getLanguage() + "_" + locale.getCountry();
        String basePath = getBasePath();
        String extension = getFileExtension();

        for (String module : getModules()) {
            String path = basePath + module + "/messages_" + localeTag + extension;
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
        Locale[] locales = getSupportedLocales();
        return locales != null ? Arrays.asList(locales) : Collections.emptyList();
    }

    @Override
    public String getTranslation(String key, Locale locale, Object... params) {
        if (key == null) {
            return "";
        }

        Map<String, String> bundle = translationsByLocale.get(locale);
        String message = bundle != null ? bundle.get(key) : null;

        if (message == null && !locale.equals(defaultLocale)) {
            // Fall back to default locale
            Map<String, String> fallback = translationsByLocale.get(defaultLocale);
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
     * Get a translation using the current session locale.
     */
    public String get(String key, Object... params) {
        Locale locale = VaadinSession.getCurrent() != null ?
                VaadinSession.getCurrent().getLocale() :
                defaultLocale;
        return getTranslation(key, locale, params);
    }

    /**
     * Get a translation for a specific locale.
     */
    public String get(String key, Locale locale, Object... params) {
        return getTranslation(key, locale, params);
    }

    /**
     * Set the current locale.
     */
    public void setLocale(Locale locale) {
        if (VaadinSession.getCurrent() != null) {
            VaadinSession.getCurrent().setLocale(locale);
        }
    }

    /**
     * Get the current locale.
     */
    public Locale getCurrentLocale() {
        return VaadinSession.getCurrent() != null ?
                VaadinSession.getCurrent().getLocale() :
                defaultLocale;
    }

    /**
     * Get the default locale.
     */
    public Locale getDefaultLocaleValue() {
        return defaultLocale;
    }

    /**
     * Reload translations for a specific locale.
     * Useful for dynamic updates.
     */
    public void reloadLocale(Locale locale) {
        translationsByLocale.put(locale, loadMergedBundle(locale));
    }

    /**
     * Reload all translations.
     */
    public void reloadAll() {
        for (Locale locale : getSupportedLocales()) {
            reloadLocale(locale);
        }
    }
}
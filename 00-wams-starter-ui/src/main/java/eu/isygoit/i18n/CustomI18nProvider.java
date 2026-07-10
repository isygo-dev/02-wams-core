package eu.isygoit.i18n;

import com.vaadin.flow.i18n.I18NProvider;

import java.util.List;
import java.util.Locale;

/**
 * Interface defining the contract for internationalization providers.
 * Provides methods for accessing translations and managing locales.
 */
public interface CustomI18nProvider extends I18NProvider {

    /**
     * Get a translation for the given key using the current session locale.
     *
     * @param key    the translation key
     * @param params optional parameters for formatting
     * @return the translated message, or the key if not found
     */
    String get(String key, Object... params);

    /**
     * Get a translation for the given key and locale.
     *
     * @param key    the translation key
     * @param locale the locale to use
     * @param params optional parameters for formatting
     * @return the translated message, or the key if not found
     */
    String get(String key, Locale locale, Object... params);

    /**
     * Get a translation using Vaadin's I18NProvider interface.
     *
     * @param key    the translation key
     * @param locale the locale to use
     * @param params optional parameters for formatting
     * @return the translated message, or the key if not found
     */
    String getTranslation(String key, Locale locale, Object... params);

    /**
     * Get the list of supported locales.
     *
     * @return list of supported locales
     */
    List<Locale> getProvidedLocales();

    /**
     * Get the current session locale.
     *
     * @return the current locale
     */
    Locale getCurrentLocale();

    /**
     * Set the current session locale.
     *
     * @param locale the locale to set
     */
    void setLocale(Locale locale);

    /**
     * Get the default locale used as fallback.
     *
     * @return the default locale
     */
    Locale getDefaultLocaleValue();

    /**
     * Check if a translation key exists.
     *
     * @param key the translation key
     * @return true if the key exists and has a translation
     */
    default boolean hasKey(String key) {
        String result = get(key);
        return result != null && !result.equals(key) && !result.equals("!" + key + "!");
    }

    /**
     * Get a translation with a default value if the key doesn't exist.
     *
     * @param key          the translation key
     * @param defaultValue the default value to use if translation is not found
     * @param params       optional parameters for formatting
     * @return the translated message or the default value
     */
    default String getWithDefault(String key, String defaultValue, Object... params) {
        String result = get(key);
        if (result == null || result.equals(key) || result.equals("!" + key + "!") || result.isBlank()) {
            if (params != null && params.length > 0) {
                return String.format(defaultValue, params);
            }
            return defaultValue;
        }
        if (params != null && params.length > 0) {
            return String.format(result, params);
        }
        return result;
    }

    /**
     * Reload translations for a specific locale.
     * Useful for dynamic updates.
     *
     * @param locale the locale to reload
     */
    void reloadLocale(Locale locale);

    /**
     * Reload all translations.
     */
    void reloadAll();

    /**
     * Get the base path for i18n resource files.
     *
     * @return the base path
     */
    String getBasePath();

    /**
     * Get the file extension for i18n resource files.
     *
     * @return the file extension
     */
    String getFileExtension();
}
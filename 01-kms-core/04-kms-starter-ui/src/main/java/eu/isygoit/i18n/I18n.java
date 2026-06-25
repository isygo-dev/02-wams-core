package eu.isygoit.i18n;

import com.vaadin.flow.server.VaadinSession;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;

/**
 * Helper class pour accéder facilement aux traductions dans les vues Vaadin.
 * Utilise le I18nProvider enregistré dans le contexte Spring.
 */
@Component
public class I18n implements ApplicationContextAware {

    private static ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        I18n.context = applicationContext;
    }

    private static I18nProvider getProvider() {
        if (context != null) {
            return context.getBean(I18nProvider.class);
        }
        return null;
    }

    /**
     * Obtient une traduction pour la clé donnée, en utilisant la locale de la session courante
     */
    public static String t(String key) {
        I18nProvider provider = getProvider();
        if (provider != null) {
            return provider.get(key);
        }
        return key;
    }

    /**
     * Obtient une traduction formatée avec les paramètres donnés
     * Utilise MessageFormat pour substituer les paramètres {0}, {1}, etc.
     */
    public static String t(String key, Object... params) {
        I18nProvider provider = getProvider();
        if (provider != null) {
            String message = provider.get(key);
            if (params != null && params.length > 0) {
                return MessageFormat.format(message, params);
            }
            return message;
        }
        if (params != null && params.length > 0) {
            return key + " " + String.join(", ", convertParamsToString(params));
        }
        return key;
    }

    /**
     * Obtient une traduction pour une locale spécifique
     */
    public static String t(String key, Locale locale) {
        I18nProvider provider = getProvider();
        if (provider != null) {
            return provider.get(key, locale);
        }
        return key;
    }

    /**
     * Obtient une traduction formatée pour une locale spécifique
     * Utilise MessageFormat pour substituer les paramètres {0}, {1}, etc.
     */
    public static String t(String key, Locale locale, Object... params) {
        I18nProvider provider = getProvider();
        if (provider != null) {
            String message = provider.get(key, locale);
            if (params != null && params.length > 0) {
                return MessageFormat.format(message, params);
            }
            return message;
        }
        if (params != null && params.length > 0) {
            return key + " " + String.join(", ", convertParamsToString(params));
        }
        return key;
    }

    /**
     * Change la locale courante
     */
    public static void setLocale(Locale locale) {
        I18nProvider provider = getProvider();
        if (provider != null) {
            provider.setLocale(locale);
        }
    }

    /**
     * Obtient la locale courante
     */
    public static Locale getCurrentLocale() {
        I18nProvider provider = getProvider();
        if (provider != null) {
            return provider.getCurrentLocale();
        }
        return new Locale("en", "US");
    }

    /**
     * Obtient la liste des locales supportées
     */
    public static List<Locale> getSupportedLocales() {
        I18nProvider provider = getProvider();
        if (provider != null) {
            return provider.getProvidedLocales();
        }
        return List.of(new Locale("en", "US"));
    }

    /**
     * Vérifie si une clé de traduction existe
     */
    public static boolean hasKey(String key) {
        I18nProvider provider = getProvider();
        if (provider != null) {
            String result = provider.get(key);
            return result != null && !result.equals(key);
        }
        return false;
    }

    /**
     * Obtient une traduction avec une valeur par défaut si la clé n'existe pas
     */
    public static String tWithDefault(String key, String defaultValue, Object... params) {
        String result = t(key, params);
        if (result == null || result.equals(key) || result.isBlank()) {
            if (params != null && params.length > 0) {
                return MessageFormat.format(defaultValue, params);
            }
            return defaultValue;
        }
        return result;
    }

    /**
     * Méthode utilitaire pour convertir les paramètres en chaînes de caractères
     */
    private static String[] convertParamsToString(Object... params) {
        String[] result = new String[params.length];
        for (int i = 0; i < params.length; i++) {
            result[i] = params[i] != null ? params[i].toString() : "null";
        }
        return result;
    }

    /**
     * Formate un message avec les paramètres donnés
     * Utile pour les messages qui ne sont pas dans les fichiers de traduction
     */
    public static String format(String message, Object... params) {
        if (params != null && params.length > 0) {
            return MessageFormat.format(message, params);
        }
        return message;
    }

    /**
     * Raccourci pour I18n.t() avec paramètres
     * Permet une syntaxe plus fluide: i18n("key", param1, param2)
     */
    public static String i18n(String key, Object... params) {
        return t(key, params);
    }
}
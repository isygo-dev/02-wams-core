package eu.isygoit.i18n;

import com.vaadin.flow.server.VaadinSession;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

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
     */
    public static String t(String key, Object... params) {
        I18nProvider provider = getProvider();
        if (provider != null) {
            return provider.get(key, params);
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
     */
    public static String t(String key, Locale locale, Object... params) {
        I18nProvider provider = getProvider();
        if (provider != null) {
            return provider.get(key, locale, params);
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
    public static java.util.List<Locale> getSupportedLocales() {
        I18nProvider provider = getProvider();
        if (provider != null) {
            return provider.getProvidedLocales();
        }
        return java.util.List.of(new Locale("en", "US"));
    }
}


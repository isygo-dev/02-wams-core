package eu.isygoit.i18n;

import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class I18nProvider extends AbstractI18nProvider {

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

    @Override
    protected String[] getModules() {
        return MODULES;
    }

    @Override
    protected Locale[] getSupportedLocales() {
        return SUPPORTED_LOCALES;
    }

    @Override
    protected Locale getDefaultLocale() {
        return new Locale("en", "US");
    }
}
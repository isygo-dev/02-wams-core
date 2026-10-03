package eu.isygoit.config;

import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import eu.isygoit.i18n.CustomI18nProvider;
import eu.isygoit.i18n.I18n;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Locale;

/**
 * Configuration Vaadin pour l'application.
 * Configure le I18nProvider pour gérer les traductions multilingues.
 */
@Configuration
@AutoConfiguration
public class VaadinConfig implements VaadinServiceInitListener {

    /**
     * Enregistre le I18nProvider comme fournisseur de traductions Vaadin
     */
    @Bean
    public I18NProvider i18nProvider(CustomI18nProvider i18nProvider) {
        return i18nProvider;
    }

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addUIInitListener(uiInitEvent -> {
            var ui = uiInitEvent.getUI();
            ui.getLoadingIndicatorConfiguration().setApplyDefaultTheme(false);

            String direction = isRightToLeft(I18n.getCurrentLocale()) ? "rtl" : "ltr";
            ui.getElement().setAttribute("dir", direction);
            ui.getPage().executeJs(
                    "document.documentElement.setAttribute('dir', $0)", direction);
        });
    }

    private boolean isRightToLeft(Locale locale) {
        return locale != null && "ar".equalsIgnoreCase(locale.getLanguage());
    }
}

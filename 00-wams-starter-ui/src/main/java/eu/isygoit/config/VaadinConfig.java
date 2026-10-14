package eu.isygoit.config;

import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import eu.isygoit.i18n.CustomI18nProvider;
import eu.isygoit.ui.common.layout.PageEnvironment;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
            // Direction (Arabic = rtl), language and Lumo light/dark theme; layouts and sign-in views apply the
            // same settings again when they are shown.
            PageEnvironment.apply(ui);
        });
    }
}

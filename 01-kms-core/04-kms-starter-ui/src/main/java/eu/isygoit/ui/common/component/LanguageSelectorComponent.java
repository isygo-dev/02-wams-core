package eu.isygoit.ui.common.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.server.VaadinSession;
import eu.isygoit.i18n.I18n;

import java.util.Locale;

/**
 * Component pour sélectionner et changer la langue de l'application.
 * Permet aux utilisateurs de basculer entre les langues supportées.
 */
public class LanguageSelectorComponent extends HorizontalLayout {

    private final ComboBox<Locale> languageCombo;

    public LanguageSelectorComponent() {
        setAlignItems(FlexComponent.Alignment.CENTER);
        setSpacing(true);
        setPadding(false);
        getStyle().set("padding", "0 var(--lumo-space-s)");

        // Add globe icon
        Icon globeIcon = VaadinIcon.GLOBE.create();
        globeIcon.setSize("18px");

        // Create language combo box
        languageCombo = new ComboBox<>();
        languageCombo.setItems(I18n.getSupportedLocales());
        languageCombo.setItemLabelGenerator(this::getLanguageName);
        languageCombo.setWidth("120px");
        languageCombo.setPlaceholder("Language");
        languageCombo.setClearButtonVisible(false);

        // Set current locale
        languageCombo.setValue(I18n.getCurrentLocale());

        // Add change listener
        languageCombo.addValueChangeListener(event -> {
            if (event.getValue() != null) {
                I18n.setLocale(event.getValue());
                VaadinSession.getCurrent().setAttribute("locale", event.getValue());
                // Refresh the entire UI to apply new language
                com.vaadin.flow.component.UI.getCurrent().getPage().reload();
            }
        });

        add(globeIcon, languageCombo);
    }

    /**
     * Get the localized name of a language
     */
    private String getLanguageName(Locale locale) {
        if (locale.getCountry().isEmpty()) {
            return locale.getDisplayLanguage(locale);
        }
        return locale.getDisplayLanguage(locale) + " (" + locale.getDisplayCountry(locale) + ")";
    }

    /**
     * Update the current language in the UI
     */
    public void updateCurrentLanguage() {
        Locale current = I18n.getCurrentLocale();
        languageCombo.setValue(current);
    }
}


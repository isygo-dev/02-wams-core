package eu.isygoit.ui.common.component;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.i18n.I18n;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Modern language selector with flag emojis and a compact, stylish design.
 * Allows users to switch between supported languages.
 */
public class LanguageSelectorComponent extends HorizontalLayout {

    private final ComboBox<Locale> languageCombo;
    private final Icon globeIcon;
    private final Map<String, String> languageToFlagMap = new HashMap<>();

    public LanguageSelectorComponent() {
        setAlignItems(FlexComponent.Alignment.CENTER);
        setSpacing(true);
        setPadding(false);
        getStyle()
                .set("padding", "0 var(--lumo-space-s)")
                .set("background", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("transition", "all 0.2s ease");

        // Add a subtle hover effect
        addAttachListener(e -> getStyle()
                .set("cursor", "pointer")
                .set("background", "var(--lumo-contrast-10pct)"));

        // Globe icon
        globeIcon = VaadinIcon.GLOBE.create();
        globeIcon.setSize("18px");
        globeIcon.getStyle().set("color", "var(--lumo-secondary-text-color)");

        // Build language → flag mapping
        initFlagMapping();

        // Create language combo box with flag support
        languageCombo = new ComboBox<>();
        languageCombo.setItems(I18n.getSupportedLocales());
        languageCombo.setRenderer(new ComponentRenderer<>(locale -> {
            HorizontalLayout itemLayout = new HorizontalLayout();
            itemLayout.setAlignItems(FlexComponent.Alignment.CENTER);
            itemLayout.setSpacing(true);

            String flag = getFlagEmoji(locale);
            Span flagSpan = new Span(flag);
            flagSpan.getStyle().set("font-size", "18px");

            String languageName = getLanguageName(locale);
            Span nameSpan = new Span(languageName);
            nameSpan.addClassName(LumoUtility.FontSize.SMALL);

            itemLayout.add(flagSpan, nameSpan);
            return itemLayout;
        }));

        languageCombo.setItemLabelGenerator(locale -> {
            String flag = getFlagEmoji(locale);
            String name = getLanguageName(locale);
            return flag + " " + name;
        });

        languageCombo.setWidth("auto");
        languageCombo.setMinWidth("140px");
        languageCombo.setPlaceholder(null);
        languageCombo.setClearButtonVisible(false);
        languageCombo.getStyle()
                .set("background", "transparent")
                .set("border", "none")
                .set("box-shadow", "none")
                .set("--lumo-combo-box-overlay-width", "auto")
                .set("min-width", "0");

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
     * Initialises the mapping from language codes to country codes for flag emojis.
     */
    private void initFlagMapping() {
        languageToFlagMap.put("en", "US");
        languageToFlagMap.put("fr", "FR");
        languageToFlagMap.put("de", "DE");
        languageToFlagMap.put("es", "ES");
        languageToFlagMap.put("it", "IT");
        languageToFlagMap.put("pt", "PT");
        languageToFlagMap.put("nl", "NL");
        languageToFlagMap.put("ru", "RU");
        languageToFlagMap.put("zh", "CN");
        languageToFlagMap.put("ja", "JP");
        languageToFlagMap.put("ko", "KR");
        languageToFlagMap.put("ar", "SA");
        languageToFlagMap.put("hi", "IN");
    }

    /**
     * Returns the flag emoji for a given locale.
     * Uses the country code if available, otherwise falls back to the mapping.
     */
    private String getFlagEmoji(Locale locale) {
        String countryCode = locale.getCountry();
        if (countryCode == null || countryCode.isEmpty()) {
            // Try to map from language code
            String language = locale.getLanguage();
            countryCode = languageToFlagMap.getOrDefault(language, "UN");
        }
        // Convert country code to uppercase flag emoji
        return countryCodeToFlagEmoji(countryCode);
    }

    /**
     * Converts a two-letter country code to its flag emoji representation.
     */
    private String countryCodeToFlagEmoji(String countryCode) {
        if (countryCode == null || countryCode.length() != 2) {
            return "🌐"; // fallback globe
        }
        String code = countryCode.toUpperCase();
        // Unicode regional indicator symbols: 🇦 = U+1F1E6, so we add offset
        int firstChar = Character.codePointAt(code, 0) - 'A' + 0x1F1E6;
        int secondChar = Character.codePointAt(code, 1) - 'A' + 0x1F1E6;
        return new String(Character.toChars(firstChar)) + new String(Character.toChars(secondChar));
    }

    /**
     * Returns the localized display name of the language.
     */
    private String getLanguageName(Locale locale) {
        if (locale.getCountry().isEmpty()) {
            return locale.getDisplayLanguage(locale);
        }
        return locale.getDisplayLanguage(locale) + " (" + locale.getDisplayCountry(locale) + ")";
    }

    /**
     * Updates the language selector to reflect the current locale.
     */
    public void updateCurrentLanguage() {
        Locale current = I18n.getCurrentLocale();
        languageCombo.setValue(current);
    }
}
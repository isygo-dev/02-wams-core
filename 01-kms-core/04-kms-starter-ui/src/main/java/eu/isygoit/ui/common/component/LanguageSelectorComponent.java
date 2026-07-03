package eu.isygoit.ui.common.component;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.i18n.I18n;

import java.util.Locale;

/**
 * Modern language selector with flag icons and a polished, compact design.
 * Uses flag-icon-css library (CDN) for reliable flag display.
 * Seamlessly integrates with Lumo theming (dark/light modes).
 */
@StyleSheet("https://cdnjs.cloudflare.com/ajax/libs/flag-icon-css/3.5.0/css/flag-icon.min.css")
public class LanguageSelectorComponent extends HorizontalLayout {

    private final ComboBox<Locale> languageCombo;
    private final Icon globeIcon;
    private final Span flagPrefixSpan;

    public LanguageSelectorComponent() {
        setAlignItems(FlexComponent.Alignment.CENTER);
        setSpacing(false);
        setPadding(false);
        addClassName("wams-language-selector");

        // Globe icon
        globeIcon = VaadinIcon.GLOBE.create();
        globeIcon.setSize("16px");
        globeIcon.addClassName("wams-language-selector__globe-icon");

        // Prefix flag (shows selected flag)
        flagPrefixSpan = new Span();
        flagPrefixSpan.addClassName("flag-icon");
        flagPrefixSpan.addClassName(getFlagCssClass(I18n.getCurrentLocale()));
        flagPrefixSpan.addClassName("wams-language-selector__flag");

        // Language combo box with flag icons
        languageCombo = new ComboBox<>();
        languageCombo.setItems(I18n.getSupportedLocales());
        languageCombo.setRenderer(new ComponentRenderer<>(locale -> {
            HorizontalLayout itemLayout = new HorizontalLayout();
            itemLayout.setAlignItems(FlexComponent.Alignment.CENTER);
            itemLayout.setSpacing(true);
            itemLayout.setPadding(false);

            // Flag span
            Span flagSpan = new Span();
            flagSpan.addClassName("flag-icon");
            flagSpan.addClassName(getFlagCssClass(locale));
            flagSpan.addClassName("wams-language-selector__item-flag");

            // Language name
            String languageName = getLanguageName(locale);
            Span nameSpan = new Span(languageName);
            nameSpan.addClassName(LumoUtility.FontSize.SMALL);
            nameSpan.addClassName("wams-language-selector__item-name");

            itemLayout.add(flagSpan, nameSpan);
            return itemLayout;
        }));

        // Show selected value as flag + language name
        languageCombo.setItemLabelGenerator(locale -> {
            String flag = getFlagEmoji(locale); // fallback if needed
            return getLanguageName(locale);
        });

        // Use flag prefix for the selected value
        languageCombo.setPrefixComponent(flagPrefixSpan);

        // Styling
        languageCombo.setWidth("auto");
        languageCombo.setMinWidth("100px");
        languageCombo.setPlaceholder(null);
        languageCombo.setClearButtonVisible(false);
        languageCombo.addClassName("wams-language-selector__combo");

        // Set current locale
        languageCombo.setValue(I18n.getCurrentLocale());

        // Add change listener – update prefix flag and reload page
        languageCombo.addValueChangeListener(event -> {
            if (event.getValue() != null) {
                // Update the prefix flag
                String newFlagClass = getFlagCssClass(event.getValue());
                flagPrefixSpan.setClassName("flag-icon wams-language-selector__flag " + newFlagClass);

                // Also update the prefix tooltip (optional)
                flagPrefixSpan.setTitle(getLanguageName(event.getValue()));

                I18n.setLocale(event.getValue());
                VaadinSession.getCurrent().setAttribute("locale", event.getValue());
                // Refresh the entire UI to apply new language
                UI.getCurrent().getPage().reload();
            }
        });

        // Add a tooltip to the prefix flag
        flagPrefixSpan.setTitle(getLanguageName(I18n.getCurrentLocale()));

        add(globeIcon, languageCombo);
    }

    /**
     * Returns the CSS class for the flag icon based on the locale.
     * Maps language to country code.
     */
    private String getFlagCssClass(Locale locale) {
        String countryCode = locale.getCountry();
        if (countryCode == null || countryCode.isEmpty()) {
            // Fallback mapping for languages without country
            countryCode = switch (locale.getLanguage()) {
                case "en" -> "us";
                case "fr" -> "fr";
                case "de" -> "de";
                case "es" -> "es";
                case "it" -> "it";
                case "pt" -> "pt";
                case "nl" -> "nl";
                case "ru" -> "ru";
                case "zh" -> "cn";
                case "ja" -> "jp";
                case "ko" -> "kr";
                case "ar" -> "sa";
                case "hi" -> "in";
                default -> "un";
            };
        }
        return "flag-icon-" + countryCode.toLowerCase();
    }

    /**
     * Returns the flag emoji as a fallback if the CSS class fails.
     */
    private String getFlagEmoji(Locale locale) {
        String countryCode = locale.getCountry();
        if (countryCode == null || countryCode.isEmpty()) {
            countryCode = switch (locale.getLanguage()) {
                case "en" -> "US";
                case "fr" -> "FR";
                case "de" -> "DE";
                case "es" -> "ES";
                case "it" -> "IT";
                case "pt" -> "PT";
                case "nl" -> "NL";
                case "ru" -> "RU";
                case "zh" -> "CN";
                case "ja" -> "JP";
                case "ko" -> "KR";
                case "ar" -> "SA";
                case "hi" -> "IN";
                default -> "UN";
            };
        }
        // Convert country code to flag emoji (regional indicator symbols)
        int firstChar = Character.codePointAt(countryCode.toUpperCase(), 0) - 'A' + 0x1F1E6;
        int secondChar = Character.codePointAt(countryCode.toUpperCase(), 1) - 'A' + 0x1F1E6;
        return new String(Character.toChars(firstChar)) + new String(Character.toChars(secondChar));
    }

    /**
     * Returns the localized display name of the language.
     */
    private String getLanguageName(Locale locale) {
        String languageCode = locale.getLanguage();
        String languageName = switch (languageCode) {
            case "en" -> I18n.t("common.language.selector.english");
            case "fr" -> I18n.t("common.language.selector.french");
            case "de" -> I18n.t("common.language.selector.german");
            case "es" -> I18n.t("common.language.selector.spanish");
            case "it" -> I18n.t("common.language.selector.italian");
            case "pt" -> I18n.t("common.language.selector.portuguese");
            case "nl" -> I18n.t("common.language.selector.dutch");
            case "ru" -> I18n.t("common.language.selector.russian");
            case "zh" -> I18n.t("common.language.selector.chinese");
            case "ja" -> I18n.t("common.language.selector.japanese");
            case "ko" -> I18n.t("common.language.selector.korean");
            case "ar" -> I18n.t("common.language.selector.arabic");
            case "hi" -> I18n.t("common.language.selector.hindi");
            default -> I18n.t("common.language.selector.fallback");
        };

        if (locale.getCountry() != null && !locale.getCountry().isEmpty()) {
            return languageName + " (" + locale.getDisplayCountry(locale) + ")";
        }
        return languageName;
    }

    /**
     * Updates the language selector to reflect the current locale.
     */
    public void updateCurrentLanguage() {
        Locale current = I18n.getCurrentLocale();
        languageCombo.setValue(current);
        // Update prefix flag
        flagPrefixSpan.setClassName("flag-icon wams-language-selector__flag " + getFlagCssClass(current));
        flagPrefixSpan.setTitle(getLanguageName(current));
    }
}
package eu.isygoit.ui.common.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.popover.Popover;
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
    // Icon-only trigger shown instead of languageCombo on mobile (see
    // .wams-language-selector__mobile-trigger in layout.css) – a shrunk
    // ComboBox still carries its own input/dropdown-arrow chrome and doesn't
    // read as "just an icon", so mobile gets a real icon button + Popover
    // instead, matching the notifications/settings buttons' pattern.
    private final Button mobileTriggerButton;
    private final Popover mobilePopover;

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
        languageCombo.setRenderer(new ComponentRenderer<>(this::buildLocaleItemContent));

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
                applyLocaleChange(event.getValue());
            }
        });

        // Add a tooltip to the prefix flag
        flagPrefixSpan.setTitle(getLanguageName(I18n.getCurrentLocale()));

        // Mobile trigger: icon-only button + Popover listing the same
        // locales, swapped in for globeIcon/languageCombo below 768px.
        mobileTriggerButton = new Button(VaadinIcon.GLOBE.create());
        mobileTriggerButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        mobileTriggerButton.addClassName("wams-header-icon-btn");
        mobileTriggerButton.addClassName("wams-language-selector__mobile-trigger");
        String languageTooltip = I18n.t("common.layout.header.language.tooltip");
        mobileTriggerButton.setTooltipText(languageTooltip);
        mobileTriggerButton.setAriaLabel(languageTooltip);

        mobilePopover = new Popover();
        mobilePopover.setTarget(mobileTriggerButton);
        mobilePopover.addClassName("wams-header-popover");
        mobilePopover.addClassName("wams-language-selector__popover");

        VerticalLayout popoverContent = new VerticalLayout();
        popoverContent.setPadding(false);
        popoverContent.setSpacing(false);
        for (Locale locale : I18n.getSupportedLocales()) {
            Div item = new Div(buildLocaleItemContent(locale));
            item.addClassName("wams-language-selector__popover-item");
            item.addClickListener(e -> applyLocaleChange(locale));
            popoverContent.add(item);
        }
        mobilePopover.add(popoverContent);

        add(globeIcon, languageCombo, mobileTriggerButton);
    }

    /**
     * Flag + language-name row shared by the desktop combo's dropdown items
     * and the mobile Popover's picker rows.
     */
    private HorizontalLayout buildLocaleItemContent(Locale locale) {
        HorizontalLayout itemLayout = new HorizontalLayout();
        itemLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        itemLayout.setSpacing(true);
        itemLayout.setPadding(false);

        Span flagSpan = new Span();
        flagSpan.addClassName("flag-icon");
        flagSpan.addClassName(getFlagCssClass(locale));
        flagSpan.addClassName("wams-language-selector__item-flag");

        Span nameSpan = new Span(getLanguageName(locale));
        nameSpan.addClassName(LumoUtility.FontSize.SMALL);
        nameSpan.addClassName("wams-language-selector__item-name");

        itemLayout.add(flagSpan, nameSpan);
        return itemLayout;
    }

    /**
     * Applies a newly-picked locale (from either the desktop combo or the
     * mobile Popover) and reloads so the whole UI re-renders in the new
     * language.
     */
    private void applyLocaleChange(Locale locale) {
        String newFlagClass = getFlagCssClass(locale);
        flagPrefixSpan.setClassName("flag-icon wams-language-selector__flag " + newFlagClass);
        flagPrefixSpan.setTitle(getLanguageName(locale));

        I18n.setLocale(locale);
        VaadinSession.getCurrent().setAttribute("locale", locale);
        UI.getCurrent().getPage().reload();
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
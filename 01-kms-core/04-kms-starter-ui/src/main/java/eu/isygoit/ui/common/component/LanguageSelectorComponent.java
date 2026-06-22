package eu.isygoit.ui.common.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import eu.isygoit.ui.common.view.ManagementVerticalView;
import eu.isygoit.ui.common.view.ManagementVerticalView;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
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
public class LanguageSelectorComponent extends HorizontalLayout {

    private final ComboBox<Locale> languageCombo;
    private final Icon globeIcon;
    private final Span flagPrefixSpan;

    public LanguageSelectorComponent() {
        // Inject flag-icon-css if not already present
        injectFlagIconCss();

        setAlignItems(FlexComponent.Alignment.CENTER);
        setSpacing(false);
        setPadding(false);
        getStyle()
                .set("padding", "2px var(--lumo-space-s)")
                .set("background", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("transition", "all 0.2s ease-in-out")
                .set("height", "var(--lumo-size-m)")
                .set("box-shadow", "var(--lumo-box-shadow-xs)");

        // Hover effect: subtle elevation and background change
        addAttachListener(e -> {
            getStyle()
                    .set("cursor", "pointer")
                    .set("transition", "all 0.25s ease-in-out");
            // On hover
            addClassName("language-selector-hover");
        });

        // We'll add hover via CSS injection instead of inline to avoid the need for a separate style.
        // We'll inject a small style block to handle hover.
        injectHoverStyle();

        // Globe icon
        globeIcon = VaadinIcon.GLOBE.create();
        globeIcon.setSize("16px");
        globeIcon.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("margin-right", "var(--lumo-space-xs)");

        // Prefix flag (shows selected flag)
        flagPrefixSpan = new Span();
        flagPrefixSpan.addClassName("flag-icon");
        flagPrefixSpan.addClassName(getFlagCssClass(I18n.getCurrentLocale()));
        flagPrefixSpan.getStyle()
                .set("font-size", "1.2em")
                .set("margin-right", "var(--lumo-space-xs)");

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
            flagSpan.getStyle().set("font-size", "1.2em");

            // Language name
            String languageName = getLanguageName(locale);
            Span nameSpan = new Span(languageName);
            nameSpan.addClassName(LumoUtility.FontSize.SMALL);
            nameSpan.getStyle().set("white-space", "nowrap");

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
        languageCombo.getStyle()
                .set("background", "transparent")
                .set("border", "none")
                .set("box-shadow", "none")
                .set("--lumo-combo-box-overlay-width", "auto")
                .set("min-width", "0")
                .set("padding", "0 var(--lumo-space-s)");

        // Set current locale
        languageCombo.setValue(I18n.getCurrentLocale());

        // Add change listener – update prefix flag and reload page
        languageCombo.addValueChangeListener(event -> {
            if (event.getValue() != null) {
                // Update the prefix flag
                String newFlagClass = getFlagCssClass(event.getValue());
                flagPrefixSpan.setClassName("flag-icon " + newFlagClass);

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
     * Injects the flag-icon-css library if not already loaded.
     * Uses cloudflare CDN for reliability.
     */
    private void injectFlagIconCss() {
        UI.getCurrent().getPage().executeJs(
                "if (!document.getElementById('flag-icon-css')) {" +
                        "  const link = document.createElement('link');" +
                        "  link.id = 'flag-icon-css';" +
                        "  link.rel = 'stylesheet';" +
                        "  link.href = 'https://cdnjs.cloudflare.com/ajax/libs/flag-icon-css/3.5.0/css/flag-icon.min.css';" +
                        "  document.head.appendChild(link);" +
                        "}"
        );
    }

    /**
     * Injects a small style block for hover effects on the component.
     */
    private void injectHoverStyle() {
        UI.getCurrent().getPage().executeJs(
                "const style = document.createElement('style');" +
                        "style.textContent = `" +
                        "  .language-selector-hover:hover {" +
                        "    background: var(--lumo-contrast-10pct) !important;" +
                        "    box-shadow: var(--lumo-box-shadow-s) !important;" +
                        "    border-color: var(--lumo-primary-color) !important;" +
                        "  }" +
                        "  .language-selector-hover:active {" +
                        "    background: var(--lumo-contrast-20pct) !important;" +
                        "    transform: scale(0.98);" +
                        "  }" +
                        "`;" +
                        "document.head.appendChild(style);"
        );
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
        // Update prefix flag
        flagPrefixSpan.setClassName("flag-icon " + getFlagCssClass(current));
        flagPrefixSpan.setTitle(getLanguageName(current));
    }
}
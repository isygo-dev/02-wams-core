package eu.isygoit.ui.common.landing;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.layout.BaseMainLayout;

import java.util.List;
import java.util.stream.IntStream;

/**
 * Abstract base class for landing pages that display modules as interactive cards.
 *
 * <p>This class handles all the UI rendering for a module-based landing page,
 * including hero section with floating decorations, cards container with module
 * cards, and footer. Subclasses must implement {@link #getModules()} to provide
 * the list of modules to display.</p>
 *
 * @see ModuleInfo
 */
public abstract class BaseLandingView extends BaseMainLayout {

    /**
     * Module record containing all metadata for a module.
     *
     * @param shortName  Display name
     * @param moduleKey  Unique identifier
     * @param icon       VaadinIcon for the module
     * @param route      Navigation route (defaults to moduleKey)
     */
    public record ModuleInfo(
            String shortName,
            String moduleKey,
            VaadinIcon icon,
            String route
    ) {
        /**
         * Convenience constructor with default route = moduleKey.
         */
        public ModuleInfo(String shortName, String moduleKey, VaadinIcon icon) {
            this(shortName, moduleKey, icon, moduleKey);
        }

        /**
         * Get the i18n prefix for this module.
         *
         * @return i18n key prefix for module-specific strings
         */
        public String getI18nPrefix() {
            return "common.landing." + moduleKey;
        }

        /**
         * Get the CSS module class name.
         *
         * @return CSS class for module theming
         */
        public String getModuleClass() {
            return "wams-module-" + moduleKey;
        }

        /**
         * Get the CSS accent class name for module colors.
         *
         * @return CSS class for accent styling
         */
        public String getAccentClass() {
            return "wams-accent-" + moduleKey;
        }
    }

    /**
     * Decor position configuration for floating icons.
     */
    private static final String[][] DECOR_POSITIONS = {
            {"-80px", "10%", "0s"},
            {"80px", "10%", "1s"},
            {"240px", "10%", "2s"},
            {"-60px", "50%", "2s"},
            {"70px", "50%", "0.5s"},
            {"190px", "10%", "1s"}
    };

    private final transient UI ui;

    /**
     * Constructor initializes the landing page with module-based content.
     */
    protected BaseLandingView() {
        this.ui = UI.getCurrent();
        addClassName("wams-landing");
        setContent(buildMainContent());
    }

    /**
     * Get the list of modules to display on the landing page.
     *
     * @return List of ModuleInfo objects
     */
    protected abstract List<ModuleInfo> getModules();

    @Override
    protected String getTitle() {
        return I18n.t("common.landing.title");
    }

    @Override
    protected Component createDrawerContent() {
        // No drawer for landing page
        return null;
    }

    @Override
    protected String getModuleKey() {
        // Landing page has no module accent
        return "";
    }

    // ─── MAIN CONTENT ────────────────────────────────────────────────────────

    private Component buildMainContent() {
        VerticalLayout main = new VerticalLayout();
        main.setPadding(false);
        main.setSpacing(false);
        main.setSizeFull();
        main.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        main.setAlignItems(FlexComponent.Alignment.CENTER);
        main.addClassName("wams-landing-main");

        main.add(buildHero());
        main.add(buildCardsContainer());
        main.add(buildFooter());

        return main;
    }

    // ─── HERO SECTION ──────────────────────────────────────────────────────

    private Div buildHero() {
        Div hero = new Div();
        hero.addClassName("wams-hero-section");

        List<ModuleInfo> modules = getModules();

        // Create floating decor for each module
        IntStream.range(0, Math.min(modules.size(), DECOR_POSITIONS.length))
                .forEach(i -> {
                    ModuleInfo module = modules.get(i);
                    String[] pos = DECOR_POSITIONS[i % DECOR_POSITIONS.length];
                    hero.add(createFloatingDecor(
                            module.icon(),
                            module.moduleKey(),
                            pos[0],
                            pos[1],
                            pos[2]
                    ));
                });

        Div titleContainer = new Div();
        titleContainer.addClassName("wams-title-container");

        H1 headline = new H1();
        headline.addClassName("wams-hero-title");
        headline.add(new Text(I18n.t("common.landing.title.main")));

        Div underline = new Div();
        underline.addClassName("wams-hero-underline");

        Paragraph subtitle = new Paragraph(I18n.t("common.landing.subtitle"));
        subtitle.addClassName(LumoUtility.TextColor.SECONDARY);
        subtitle.addClassName("wams-subtitle-text");

        Div scrollIndicator = new Div();
        scrollIndicator.addClassName("wams-scroll-indicator");

        Icon scrollIcon = VaadinIcon.ANGLE_DOUBLE_DOWN.create();
        scrollIcon.setSize("24px");
        scrollIcon.setColor("var(--lumo-secondary-text-color)");
        scrollIndicator.add(scrollIcon);
        scrollIndicator.addClickListener(e -> UI.getCurrent().getPage().executeJs(
                "document.querySelector('.wams-cards-container').scrollIntoView({ behavior: 'smooth', block: 'start' });"
        ));

        titleContainer.add(headline);
        hero.add(titleContainer, underline, subtitle, scrollIndicator);

        return hero;
    }

    private Div createFloatingDecor(VaadinIcon icon, String moduleKey, String offsetX, String offsetY, String delay) {
        Div decor = new Div();
        decor.addClassName("wams-floating-decor");
        decor.addClassName("wams-module-" + moduleKey);
        decor.getStyle()
                .set("left", offsetX)
                .set("top", offsetY)
                .set("--wams-delay", delay);

        Icon decorIcon = icon.create();
        decorIcon.setSize("40px");
        decor.add(decorIcon);

        return decor;
    }

    // ─── CARDS CONTAINER ──────────────────────────────────────────────────

    private HorizontalLayout buildCardsContainer() {
        HorizontalLayout container = new HorizontalLayout();
        container.addClassName("wams-cards-container");
        container.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        container.setAlignItems(FlexComponent.Alignment.STRETCH);
        container.setSpacing(true);

        List<ModuleInfo> modules = getModules();

        IntStream.range(0, modules.size())
                .forEach(i -> {
                    ModuleInfo module = modules.get(i);
                    String delay = (0.1 * (i + 1)) + "s";
                    container.add(createCard(module, delay));
                });

        return container;
    }

    private Div createCard(ModuleInfo module, String delay) {
        String i18nPrefix = module.getI18nPrefix();

        Div card = new Div();
        card.addClassName("wams-domain-card");
        card.addClassName(module.getModuleClass());
        card.getStyle().set("--wams-delay", delay);

        // Accent bar
        Div accentBar = new Div();
        accentBar.addClassName("wams-accent-bar");
        card.add(accentBar);

        // Card content
        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(false);
        content.setAlignItems(FlexComponent.Alignment.CENTER);
        content.addClassName("wams-domain-card-content");

        // Icon
        Icon cardIcon = module.icon().create();
        cardIcon.setSize("28px");
        cardIcon.addClassName("wams-card-icon");

        // Badge
        Div badge = new Div(module.shortName());
        badge.addClassName("wams-card-badge");

        // Title
        H2 cardTitle = new H2(I18n.t(i18nPrefix + ".title"));
        cardTitle.addClassName("wams-card-title");

        // Description
        Paragraph desc = new Paragraph(I18n.t(i18nPrefix + ".description"));
        desc.addClassName(LumoUtility.TextColor.SECONDARY);
        desc.addClassName("wams-card-description");

        // Features
        Div featuresContainer = buildFeaturesContainer(i18nPrefix);

        // Enter button
        Button enterBtn = new Button(I18n.t(i18nPrefix + ".button"), VaadinIcon.ARROW_RIGHT.create());
        enterBtn.addClassName("wams-enter-button");
        enterBtn.addClassName("wams-enter-button--accent");
        enterBtn.addClassName(LumoUtility.Width.FULL);
        enterBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        enterBtn.addClickListener(e -> navigateTo(module.route()));

        // Card click handler
        card.addClickListener(e -> {
            triggerRippleEffect(card, e.getClientX(), e.getClientY());
            navigateTo(module.route());
        });

        // Assemble content
        content.add(cardIcon, badge, cardTitle, desc, featuresContainer, enterBtn);
        card.add(content);

        return card;
    }

    /**
     * Builds the features container with chips for each feature.
     * Supports dynamic number of features (1-3+).
     *
     * @param i18nPrefix The i18n prefix for feature keys
     * @return Div containing feature chips
     */
    private Div buildFeaturesContainer(String i18nPrefix) {
        Div featuresContainer = new Div();
        featuresContainer.addClassName("wams-features-container");

        // Try to load up to 3 features
        String[] featureKeys = {".feature.1", ".feature.2", ".feature.3"};
        boolean hasFeatures = false;

        for (String key : featureKeys) {
            String fullKey = i18nPrefix + key;
            String featureText = I18n.t(fullKey);
            // Check if the translation exists (not the key itself)
            if (!featureText.equals(fullKey)) {
                Div chip = new Div(featureText);
                chip.addClassName("wams-feature-chip");
                featuresContainer.add(chip);
                hasFeatures = true;
            }
        }

        // If no features were loaded, add default ones
        if (!hasFeatures) {
            Div chip1 = new Div("Feature 1");
            chip1.addClassName("wams-feature-chip");
            Div chip2 = new Div("Feature 2");
            chip2.addClassName("wams-feature-chip");
            featuresContainer.add(chip1, chip2);
        }

        return featuresContainer;
    }

    /**
     * Ripple effect positioned at the click point.
     *
     * @param card     The card element
     * @param clientX  X coordinate of the click
     * @param clientY  Y coordinate of the click
     */
    private void triggerRippleEffect(Div card, double clientX, double clientY) {
        UI.getCurrent().getPage().executeJs(
                "const rect = $0.getBoundingClientRect();" +
                        "const ripple = document.createElement('div');" +
                        "ripple.className = 'wams-ripple-effect';" +
                        "ripple.style.left = ($1 - rect.left) + 'px';" +
                        "ripple.style.top = ($2 - rect.top) + 'px';" +
                        "$0.appendChild(ripple);" +
                        "setTimeout(() => ripple.remove(), 600);",
                card.getElement(), clientX, clientY
        );
    }

    /**
     * Navigate to the specified route.
     *
     * @param route The route to navigate to
     */
    private void navigateTo(String route) {
        if (ui != null) {
            ui.navigate(route);
        } else {
            UI.getCurrent().navigate(route);
        }
    }

    // ─── FOOTER ──────────────────────────────────────────────────────────────

    private Div buildFooter() {
        Div footer = new Div();
        footer.addClassName("wams-landing-footer");

        Paragraph footerText = new Paragraph(I18n.t("common.landing.footer"));
        footerText.addClassName(LumoUtility.FontSize.XXSMALL);
        footerText.addClassName(LumoUtility.TextColor.TERTIARY);
        footerText.addClassName("wams-footer-text");

        footer.add(footerText);
        return footer;
    }

    // ─── UTILITY METHODS ──────────────────────────────────────────────────

    /**
     * Get a module by its key.
     *
     * @param key The module key to look up
     * @return The ModuleInfo if found, null otherwise
     */
    public ModuleInfo getModuleByKey(String key) {
        return getModules().stream()
                .filter(m -> m.moduleKey().equals(key))
                .findFirst()
                .orElse(null);
    }

    /**
     * Get the icon for a module key.
     *
     * @param moduleKey The module key
     * @return The VaadinIcon for the module, or QUESTION if not found
     */
    public VaadinIcon getIconForModule(String moduleKey) {
        ModuleInfo module = getModuleByKey(moduleKey);
        return module != null ? module.icon() : VaadinIcon.QUESTION;
    }

    /**
     * Check if a module exists.
     *
     * @param moduleKey The module key to check
     * @return true if the module exists, false otherwise
     */
    public boolean moduleExists(String moduleKey) {
        return getModuleByKey(moduleKey) != null;
    }
}
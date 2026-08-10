package eu.isygoit.ui.common.landing;

import com.vaadin.flow.component.Component;
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
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.layout.BaseMainLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for landing pages that display modules as interactive cards.
 *
 * <p>This class handles all the UI rendering for a module-based landing page:
 * a hero section, a cards container with one card per module, and a footer.
 * Subclasses must implement {@link #getModules()} to provide the list of
 * modules to display.</p>
 * <p>
 * If only one module is returned by {@link #getModules()}, the view skips the
 * landing page entirely and goes straight to the same redirect-with-progress
 * screen used when picking a module from a multi-module landing page — one
 * mechanism ({@link #redirectToModule(ModuleInfo)}) drives both cases, so the
 * navigation experience (a brief "Redirecting to..." screen before moving on)
 * is identical whether there's one module or several.
 * </p>
 *
 * @see ModuleInfo
 */
public abstract class BaseLandingView extends BaseMainLayout {

    private final transient UI ui;
    private final List<CardEntry> cardEntries = new ArrayList<>();
    private Div emptyState;

    /**
     * Constructor initializes the landing page with module-based content.
     * If only one module exists, redirects directly to that module's route.
     */
    protected BaseLandingView() {
        this.ui = UI.getCurrent();
        addClassName("wams-landing");

        List<ModuleInfo> modules = getModules();

        // If only one module exists, redirect directly to it
        if (modules != null && modules.size() == 1) {
            redirectToModule(modules.get(0));
            return;
        }

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

    /**
     * Shows the "Redirecting to..." progress screen, then navigates to the
     * module's route. Used both when {@link #getModules()} returns a single
     * module (the whole landing page is skipped in favor of this) and when a
     * module is picked from a multi-module landing page (card or "Enter"
     * button), so the transition looks and behaves the same either way.
     *
     * <p>Navigates through Vaadin's own client-side router ({@link UI#navigate})
     * rather than assigning {@code window.location} directly: the latter is a
     * bare relative path (e.g. "kms", not "/kms"), so the browser resolves it
     * against whatever the current URL happens to be (trailing slash and all)
     * instead of the app's actual route — and even when it does land on the
     * right place, it forces a full page reload instead of an in-app
     * navigation. The 400ms visual delay (so the "Redirecting to..." screen
     * is actually visible) is kept, just driven by a client-side promise that
     * calls back into the server to do the real navigation once it resolves.
     *
     * @param module The module to redirect to
     */
    private void redirectToModule(ModuleInfo module) {
        // Show loading indicator before redirect
        setContent(buildRedirectingContent(module));

        ui.getPage()
                .executeJs("return new Promise(resolve => setTimeout(resolve, 400));")
                .then(ignored -> ui.navigate(module.route()));
    }

    /**
     * Builds a loading indicator page for redirection.
     *
     * @param module The module being redirected to
     * @return Component with loading indicator
     */
    private Component buildRedirectingContent(ModuleInfo module) {
        VerticalLayout loadingLayout = new VerticalLayout();
        loadingLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        loadingLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        loadingLayout.setSizeFull();
        loadingLayout.addClassName("wams-redirecting");

        // Icon
        Icon moduleIcon = module.icon().create();
        moduleIcon.setSize("48px");
        moduleIcon.addClassName("wams-redirect-icon");

        // Redirecting text
        H2 redirectingText = new H2(I18n.t("common.landing.redirecting", module.shortName()));
        redirectingText.addClassName(LumoUtility.TextColor.PRIMARY);
        redirectingText.addClassName("wams-redirect-title");

        // Subtitle
        Paragraph subtitle = new Paragraph(I18n.t("common.landing.redirecting.subtitle"));
        subtitle.addClassName(LumoUtility.TextColor.SECONDARY);
        subtitle.addClassName("wams-redirect-subtitle");

        // Progress bar
        ProgressBar progressBar = new ProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setWidth("300px");
        progressBar.addClassName("wams-redirect-progress");

        loadingLayout.add(moduleIcon, redirectingText, subtitle, progressBar);
        return loadingLayout;
    }

    private Component buildMainContent() {
        VerticalLayout main = new VerticalLayout();
        main.setPadding(false);
        main.setSpacing(false);
        main.setSizeFull();
        // Top-aligned rather than centered: centering a flex column whose
        // content (hero + card grid + footer) is taller than the viewport
        // pushes the overflow symmetrically above the box as well as below
        // it — since the page can't scroll to a negative position, the top
        // portion (hero + first card row) becomes permanently unreachable.
        // Starting from the top means any overflow spills past the bottom
        // instead, where normal scrolling already reaches it, regardless of
        // viewport size or module count.
        main.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        main.setAlignItems(FlexComponent.Alignment.CENTER);
        main.addClassName("wams-landing-main");

        main.add(buildHero());
        main.add(buildSearchField());
        main.add(buildCardsContainer());
        main.add(buildFooter());

        return main;
    }

    // ─── AREA FILTER ─────────────────────────────────────────────────────────

    private TextField buildSearchField() {
        TextField searchField = new TextField();
        searchField.addClassName("wams-landing-search");
        searchField.setPlaceholder(I18n.t("common.landing.search.placeholder"));
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.EAGER);
        searchField.addValueChangeListener(e -> filterCards(e.getValue()));
        return searchField;
    }

    private void filterCards(String query) {
        String needle = query == null ? "" : query.trim().toLowerCase();
        boolean anyVisible = false;
        for (CardEntry entry : cardEntries) {
            boolean matches = needle.isEmpty() || entry.searchText().contains(needle);
            entry.card().setVisible(matches);
            anyVisible = anyVisible || matches;
        }
        if (emptyState != null) {
            emptyState.setVisible(!anyVisible);
        }
    }

    private Div buildEmptyState() {
        Div empty = new Div();
        empty.addClassName("wams-landing-empty");
        empty.setVisible(false);

        Icon icon = VaadinIcon.SEARCH_MINUS.create();
        icon.setSize("32px");
        icon.addClassName("wams-landing-empty__icon");

        H2 title = new H2(I18n.t("common.landing.search.empty.title"));
        title.addClassName("wams-landing-empty__title");

        Paragraph description = new Paragraph(I18n.t("common.landing.search.empty.description"));
        description.addClassName(LumoUtility.TextColor.SECONDARY);

        empty.add(icon, title, description);
        return empty;
    }

    private String buildSearchText(ModuleInfo module) {
        String i18nPrefix = module.getI18nPrefix();
        return String.join(" ",
                module.shortName(),
                module.moduleKey(),
                I18n.t(i18nPrefix + ".title"),
                I18n.t(i18nPrefix + ".description")
        ).toLowerCase();
    }

    // ─── HERO SECTION ──────────────────────────────────────────────────────

    private Div buildHero() {
        Div hero = new Div();
        hero.addClassName("wams-hero-section");

        H1 headline = new H1(I18n.t("common.landing.title.main"));
        headline.addClassName("wams-hero-title");

        Paragraph subtitle = new Paragraph(I18n.t("common.landing.subtitle"));
        subtitle.addClassName(LumoUtility.TextColor.SECONDARY);
        subtitle.addClassName("wams-subtitle-text");

        hero.add(headline, subtitle);
        return hero;
    }

    // ─── CARDS CONTAINER ──────────────────────────────────────────────────

    private Component buildCardsContainer() {
        HorizontalLayout container = new HorizontalLayout();
        container.addClassName("wams-cards-container");
        container.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        container.setAlignItems(FlexComponent.Alignment.STRETCH);
        container.setSpacing(true);

        getModules().forEach(module -> {
            Div card = createCard(module);
            container.add(card);
            cardEntries.add(new CardEntry(card, buildSearchText(module)));
        });

        emptyState = buildEmptyState();

        VerticalLayout wrapper = new VerticalLayout(container, emptyState);
        wrapper.setPadding(false);
        wrapper.setSpacing(false);
        wrapper.setWidthFull();
        wrapper.setAlignItems(FlexComponent.Alignment.CENTER);
        return wrapper;
    }

    private Div createCard(ModuleInfo module) {
        String i18nPrefix = module.getI18nPrefix();

        Div card = new Div();
        card.addClassName("wams-domain-card");
        card.addClassName(module.getModuleClass());

        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(false);
        content.setAlignItems(FlexComponent.Alignment.CENTER);
        content.addClassName("wams-domain-card-content");

        Icon cardIcon = module.icon().create();
        cardIcon.setSize("28px");
        cardIcon.addClassName("wams-card-icon");

        Div badge = new Div(module.shortName());
        badge.addClassName("wams-card-badge");

        H2 cardTitle = new H2(I18n.t(i18nPrefix + ".title"));
        cardTitle.addClassName("wams-card-title");

        Paragraph desc = new Paragraph(I18n.t(i18nPrefix + ".description"));
        desc.addClassName(LumoUtility.TextColor.SECONDARY);
        desc.addClassName("wams-card-description");

        Div featuresContainer = buildFeaturesContainer(i18nPrefix);

        Button enterBtn = new Button(I18n.t(i18nPrefix + ".button"), VaadinIcon.ARROW_RIGHT.create());
        enterBtn.addClassName("wams-enter-button");
        enterBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        enterBtn.addClickListener(e -> redirectToModule(module));

        card.addClickListener(e -> redirectToModule(module));

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

        for (String key : new String[]{".feature.1", ".feature.2", ".feature.3"}) {
            String fullKey = i18nPrefix + key;
            String featureText = I18n.t(fullKey);
            // Skip keys with no translation (I18n.t returns the key itself).
            if (!featureText.equals(fullKey)) {
                Div chip = new Div(featureText);
                chip.addClassName("wams-feature-chip");
                featuresContainer.add(chip);
            }
        }

        return featuresContainer;
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

    /**
     * Module record containing all metadata for a module.
     *
     * @param shortName Display name
     * @param moduleKey Unique identifier
     * @param icon      VaadinIcon for the module
     * @param route     Navigation route (defaults to moduleKey)
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
    }

    /**
     * Pairs a rendered card with its pre-computed, lower-cased searchable
     * text (name/key/title/description), so {@link #filterCards} can match
     * against it without re-resolving i18n strings on every keystroke.
     */
    private record CardEntry(Div card, String searchText) {
    }
}

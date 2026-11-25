package eu.isygoit.ui.common.landing;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.layout.BaseMainLayout;
import eu.isygoit.ui.common.view.ViewTitleUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Abstract base class for landing pages that display modules as interactive cards.
 *
 * <p>This class handles the UI rendering for a module-based landing page:
 * a hero section, a cards container with one card per module, and a footer.
 * Subclasses provide the list of modules through {@link #getModules()}.</p>
 *
 * <p>Each {@link ModuleInfo} contains a Vaadin {@link Component} as its icon.
 * Subclasses must create fresh module definitions and fresh icon components
 * for each call to {@link #getModules()}, so components are never shared
 * between UIs or component trees.</p>
 *
 * <p>If only one module is returned, the view redirects directly to that module.</p>
 *
 * @see ModuleInfo
 */
public abstract class BaseLandingView extends BaseMainLayout implements HasDynamicTitle {

    private final transient UI ui;
    private final List<CardEntry> cardEntries = new ArrayList<>();
    private Div emptyState;

    /**
     * Creates a new image component for a module icon.
     *
     * <p>For example, a file located at
     * {@code META-INF/resources/icons/edyra-pilot.svg} is served at
     * {@code /icons/edyra-pilot.svg}.</p>
     *
     * @param path image URL relative to the application context
     * @param alt alternative text
     * @return a new Image component
     */
    public static Image createModuleImage(String path, String alt) {
        Image image = new Image(path, alt);
        image.setWidth("48px");
        image.setHeight("48px");
        image.getStyle()
                .set("object-fit", "contain")
                .set("display", "block")
                .set("flex-shrink", "0");
        return image;
    }

    /**
     * Initializes the landing page with module-based content.
     * If only one module exists, redirects directly to its route.
     *
     * <p>Important: {@link #getModules()} is invoked during construction.
     * Implementations must not depend on subclass instance fields because
     * those fields are initialized only after the superclass constructor.</p>
     */
    protected BaseLandingView() {
        this.ui = Objects.requireNonNull(UI.getCurrent(), "A current Vaadin UI is required");
        addClassName("wams-landing");

        List<ModuleInfo> modules = List.copyOf(
                Objects.requireNonNull(getModules(), "getModules() must not return null")
        );

        if (modules.size() == 1) {
            redirectToModule(modules.get(0));
            return;
        }

        setContent(buildMainContent(modules));
    }

    /**
     * Gets the modules displayed on the landing page.
     *
     * @return non-null module definitions
     */
    protected abstract List<ModuleInfo> getModules();

    @Override
    protected String getTitle() {
        return I18n.t("common.landing.title");
    }

    @Override
    protected Component createDrawerContent() {
        return null;
    }

    @Override
    protected String getModuleKey() {
        return "";
    }

    @Override
    public String getPageTitle() {
        return ViewTitleUtils.humanize(getClass());
    }

    /**
     * Shows the redirecting screen, then navigates to the selected module.
     *
     * @param module module to open
     */
    private void redirectToModule(ModuleInfo module) {
        setContent(buildRedirectingContent(module));

        ui.getPage()
                .executeJs("return new Promise(resolve => setTimeout(resolve, 400));")
                .then(ignored -> ui.navigate(module.route()));
    }

    /**
     * Builds the loading screen displayed before navigation.
     *
     * @param module module being opened
     * @return redirecting content
     */
    private Component buildRedirectingContent(ModuleInfo module) {
        VerticalLayout loadingLayout = new VerticalLayout();
        loadingLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        loadingLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        loadingLayout.setSizeFull();
        loadingLayout.addClassName("wams-redirecting");

        Component moduleIcon = module.icon();
        applyIconStyle(moduleIcon, "48px", "wams-redirect-icon");

        H2 redirectingText = new H2(
                I18n.t("common.landing.redirecting", module.shortName())
        );
        redirectingText.addClassName(LumoUtility.TextColor.PRIMARY);
        redirectingText.addClassName("wams-redirect-title");

        Paragraph subtitle = new Paragraph(
                I18n.t("common.landing.redirecting.subtitle")
        );
        subtitle.addClassName(LumoUtility.TextColor.SECONDARY);
        subtitle.addClassName("wams-redirect-subtitle");

        ProgressBar progressBar = new ProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setWidth("300px");
        progressBar.addClassName("wams-redirect-progress");

        loadingLayout.add(moduleIcon, redirectingText, subtitle, progressBar);
        return loadingLayout;
    }

    /**
     * Applies common sizing and styling to any Vaadin icon component.
     * Works with Image, Icon, and other Component implementations.
     *
     * @param icon component to style
     * @param size CSS width and height
     * @param cssClass CSS class to apply
     */
    private void applyIconStyle(Component icon, String size, String cssClass) {
        icon.getElement().getStyle()
                .set("width", size)
                .set("height", size)
                .set("flex-shrink", "0");
        icon.addClassName(cssClass);
    }

    private Component buildMainContent(List<ModuleInfo> modules) {
        VerticalLayout main = new VerticalLayout();
        main.setPadding(false);
        main.setSpacing(false);
        main.setSizeFull();
        main.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        main.setAlignItems(FlexComponent.Alignment.CENTER);
        main.addClassName("wams-landing-main");

        main.add(buildHero());
        main.add(buildSearchField());
        main.add(buildCardsContainer(modules));
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

        Paragraph description = new Paragraph(
                I18n.t("common.landing.search.empty.description")
        );
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

    // ─── HERO SECTION ─────────────────────────────────────────────────────────

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

    // ─── CARDS CONTAINER ──────────────────────────────────────────────────────

    private Component buildCardsContainer(List<ModuleInfo> modules) {
        HorizontalLayout container = new HorizontalLayout();
        container.addClassName("wams-cards-container");
        container.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        container.setAlignItems(FlexComponent.Alignment.STRETCH);
        container.setSpacing(true);

        for (ModuleInfo module : modules) {
            Div card = createCard(module);
            container.add(card);
            cardEntries.add(new CardEntry(card, buildSearchText(module)));
        }

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

        Component cardIcon = module.icon();
        applyIconStyle(cardIcon, "28px", "wams-card-icon");

        Div badge = new Div(module.shortName());
        badge.addClassName("wams-card-badge");

        H2 cardTitle = new H2(I18n.t(i18nPrefix + ".title"));
        cardTitle.addClassName("wams-card-title");

        Paragraph desc = new Paragraph(I18n.t(i18nPrefix + ".description"));
        desc.addClassName(LumoUtility.TextColor.SECONDARY);
        desc.addClassName("wams-card-description");

        Div featuresContainer = buildFeaturesContainer(i18nPrefix);

        Button enterBtn = new Button(
                I18n.t(i18nPrefix + ".button"),
                VaadinIcon.ARROW_RIGHT.create()
        );
        enterBtn.addClassName("wams-enter-button");
        enterBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        enterBtn.addClickListener(e -> redirectToModule(module));

        card.addClickListener(e -> redirectToModule(module));

        content.add(cardIcon, badge, cardTitle, desc, featuresContainer, enterBtn);
        card.add(content);

        return card;
    }

    /**
     * Builds the feature chips for a module.
     *
     * @param i18nPrefix module translation prefix
     * @return feature container
     */
    private Div buildFeaturesContainer(String i18nPrefix) {
        Div featuresContainer = new Div();
        featuresContainer.addClassName("wams-features-container");

        for (String key : new String[]{".feature.1", ".feature.2", ".feature.3"}) {
            String fullKey = i18nPrefix + key;
            String featureText = I18n.t(fullKey);

            if (!featureText.equals(fullKey)) {
                Div chip = new Div(featureText);
                chip.addClassName("wams-feature-chip");
                featuresContainer.add(chip);
            }
        }

        return featuresContainer;
    }

    // ─── FOOTER ───────────────────────────────────────────────────────────────

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

    // ─── UTILITY METHODS ──────────────────────────────────────────────────────

    /**
     * Gets a module by its key.
     *
     * @param key module key
     * @return module definition, or null if not found
     */
    public ModuleInfo getModuleByKey(String key) {
        return getModules().stream()
                .filter(module -> module.moduleKey().equals(key))
                .findFirst()
                .orElse(null);
    }

    /**
     * Gets the icon component for a module key.
     *
     * <p>The returned component is a Vaadin component. The caller becomes
     * responsible for attaching it to exactly one component tree.</p>
     *
     * @param moduleKey module key
     * @return module icon, or the default question icon
     */
    public Component getIconForModule(String moduleKey) {
        ModuleInfo module = getModuleByKey(moduleKey);
        return module != null ? module.icon() : VaadinIcon.QUESTION.create();
    }

    /**
     * Module metadata and its Vaadin component icon.
     *
     * <p>Do not share a ModuleInfo (or its icon component) between UIs or
     * component trees. {@link #getModules()} should return fresh ModuleInfo
     * instances with fresh icon components on every invocation.</p>
     *
     * @param shortName display name
     * @param moduleKey unique identifier
     * @param icon Vaadin component used as the module icon (Image, Icon, etc.)
     * @param route navigation route; defaults to moduleKey
     */
    public record ModuleInfo(
            String shortName,
            String moduleKey,
            Component icon,
            String route
    ) {
        public ModuleInfo {
            Objects.requireNonNull(shortName, "shortName must not be null");
            Objects.requireNonNull(moduleKey, "moduleKey must not be null");
            Objects.requireNonNull(icon, "icon must not be null");
            Objects.requireNonNull(route, "route must not be null");
        }

        /**
         * Convenience constructor with route defaulting to moduleKey.
         */
        public ModuleInfo(String shortName, String moduleKey, Component icon) {
            this(shortName, moduleKey, icon, moduleKey);
        }

        public String getI18nPrefix() {
            return "common.landing." + moduleKey;
        }

        public String getModuleClass() {
            return "wams-module-" + moduleKey;
        }
    }

    /**
     * Associates a rendered card with its precomputed searchable text.
     */
    private record CardEntry(Div card, String searchText) {
    }
}

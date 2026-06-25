package eu.isygoit.ui;

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
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.layout.BaseMainLayout;
import eu.isygoit.util.SecurityUtils;
import jakarta.annotation.security.PermitAll;

@Route("landing")
@PageTitle("Platform - Choose Your Area")
@PermitAll
public class LandingView extends BaseMainLayout implements BeforeEnterObserver {

    private final transient UI ui;

    public LandingView() {
        this.ui = UI.getCurrent();

        // Set the main content
        setContent(buildMainContent());

        injectResponsiveStyles();
    }

    @Override
    protected String getTitle() {
        return I18n.t("landing.title");
    }

    @Override
    protected void createDrawer() {
        // No drawer for landing page - this is a public/landing page
        // Override to keep it empty
    }

    // ─── MAIN CONTENT ────────────────────────────────────────────────────────

    private Component buildMainContent() {
        VerticalLayout main = new VerticalLayout();
        main.setPadding(false);
        main.setSpacing(false);
        main.setSizeFull();
        main.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        main.setAlignItems(FlexComponent.Alignment.CENTER);
        main.addClassName("landing-main");
        main.getStyle()
                .set("flex", "1")
                .set("padding", "var(--lumo-space-l) var(--lumo-space-m)")
                .set("max-width", "1200px")
                .set("width", "100%")
                .set("margin", "0 auto")
                .set("min-height", "calc(100vh - 80px)");

        Div hero = buildHero();
        main.add(hero);

        HorizontalLayout cardsContainer = buildCardsContainer();
        main.add(cardsContainer);

        Div footer = buildFooter();
        main.add(footer);

        return main;
    }

    // ─── REDESIGNED HERO TITLE ──────────────────────────────────────────────

    private Div buildHero() {
        Div hero = new Div();
        hero.addClassName("hero-section");
        hero.getStyle()
                .set("text-align", "center")
                .set("margin-bottom", "var(--lumo-space-xl)")
                .set("width", "100%")
                .set("padding", "var(--lumo-space-m) 0")
                .set("position", "relative");

        // ── Floating decorative elements ──
        Div decorLeft = createFloatingDecor(VaadinIcon.KEY, "var(--lumo-primary-color-30pct)", "-100px", "15%");
        Div decorRight = createFloatingDecor(VaadinIcon.USERS, "var(--lumo-success-color-30pct)", "100px", "15%");
        Div decorBottomLeft = createFloatingDecor(VaadinIcon.SHIELD, "var(--lumo-primary-color-20pct)", "-80px", "55%");
        Div decorBottomRight = createFloatingDecor(VaadinIcon.CODE, "var(--lumo-success-color-20pct)", "80px", "55%");

        hero.add(decorLeft, decorRight, decorBottomLeft, decorBottomRight);

        // ── Main Title with gradient styling ──
        Div titleContainer = new Div();
        titleContainer.getStyle()
                .set("position", "relative")
                .set("display", "inline-block")
                .set("margin-bottom", "var(--lumo-space-s)");

        H1 headline = new H1();
        headline.addClassName("hero-title");
        headline.getStyle()
                .set("font-size", "var(--lumo-font-size-xxxl)")
                .set("font-weight", "800")
                .set("margin", "0")
                .set("line-height", "1.2")
                .set("letter-spacing", "-0.02em")
                .set("position", "relative")
                .set("z-index", "1");

        // Build the title with styled parts
        Text manageText = new Text("Manage your ");

        Span highlightText = new Span("Security");
        highlightText.getStyle()
                .set("background", "linear-gradient(135deg, var(--lumo-primary-color), #7c3aed)")
                .set("-webkit-background-clip", "text")
                .set("-webkit-text-fill-color", "transparent")
                .set("background-clip", "text")
                .set("position", "relative");

        Text andText = new Text(" & ");

        Span identityText = new Span("Identity");
        identityText.getStyle()
                .set("background", "linear-gradient(135deg, var(--lumo-success-color), #059669)")
                .set("-webkit-background-clip", "text")
                .set("-webkit-text-fill-color", "transparent")
                .set("background-clip", "text")
                .set("position", "relative");

        headline.add(manageText, highlightText, andText, identityText);

        // ── Decorative underline with glow ──
        Div underline = new Div();
        underline.addClassName("hero-underline");
        underline.getStyle()
                .set("width", "140px")
                .set("height", "4px")
                .set("background", "linear-gradient(90deg, var(--lumo-primary-color), var(--lumo-success-color))")
                .set("border-radius", "var(--lumo-border-radius-s)")
                .set("margin", "var(--lumo-space-s) auto 0")
                .set("position", "relative");

        // Glow effect on underline
        Div glow = new Div();
        glow.getStyle()
                .set("position", "absolute")
                .set("top", "-2px")
                .set("left", "-20px")
                .set("right", "-20px")
                .set("height", "8px")
                .set("background", "linear-gradient(90deg, transparent, var(--lumo-primary-color-30pct), var(--lumo-success-color-30pct), transparent)")
                .set("filter", "blur(8px)")
                .set("border-radius", "var(--lumo-border-radius-s)");
        underline.add(glow);

        // ── Subtitle with icon ──
        HorizontalLayout subtitleLayout = new HorizontalLayout();
        subtitleLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        subtitleLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        subtitleLayout.setSpacing(true);
        subtitleLayout.getStyle()
                .set("margin-top", "var(--lumo-space-m)")
                .set("gap", "var(--lumo-space-s)");

        Icon arrowIcon = VaadinIcon.ARROW_DOWN.create();
        arrowIcon.setSize("20px");
        arrowIcon.setColor("var(--lumo-secondary-text-color)");

        Paragraph subtitle = new Paragraph(I18n.t("landing.subtitle"));
        subtitle.addClassName(LumoUtility.TextColor.SECONDARY);
        subtitle.getStyle()
                .set("font-size", "var(--lumo-font-size-l)")
                .set("margin", "0")
                .set("font-weight", "400")
                .set("letter-spacing", "0.01em");

        subtitleLayout.add(arrowIcon, subtitle);

        titleContainer.add(headline, underline);
        hero.add(titleContainer, subtitleLayout);

        return hero;
    }

    private Div createFloatingDecor(VaadinIcon icon, String color, String offsetX, String offsetY) {
        Div decor = new Div();
        decor.addClassName("floating-decor");
        decor.getStyle()
                .set("position", "absolute")
                .set("opacity", "0.15")
                .set("pointer-events", "none")
                .set("animation", "float 6s ease-in-out infinite")
                .set("animation-delay", Math.random() * 2 + "s");

        Icon decorIcon = icon.create();
        decorIcon.setSize("48px");
        decorIcon.setColor(color);

        decor.add(decorIcon);

        // Position with media query awareness
        decor.getStyle()
                .set("left", offsetX)
                .set("top", offsetY)
                .set("transform", "translate(" + offsetX + ", " + offsetY + ")");

        return decor;
    }

    // ─── CARDS ──────────────────────────────────────────────────────────────

    private HorizontalLayout buildCardsContainer() {
        HorizontalLayout container = new HorizontalLayout();
        container.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        container.setAlignItems(FlexComponent.Alignment.STRETCH);
        container.setSpacing(true);
        container.addClassName("cards-container");
        container.getStyle()
                .set("flex-wrap", "wrap")
                .set("gap", "var(--lumo-space-l)")
                .set("width", "100%")
                .set("justify-content", "center");

        container.add(createCard(
                "KMS",
                VaadinIcon.KEY,
                I18n.t("landing.kms.title"),
                I18n.t("landing.kms.description"),
                new String[]{
                        I18n.t("landing.kms.feature.encrypt"),
                        I18n.t("landing.kms.feature.lifecycle"),
                        I18n.t("landing.kms.feature.tokens"),
                        I18n.t("landing.kms.feature.policies")
                },
                "kms",
                ButtonVariant.LUMO_PRIMARY
        ));

        container.add(createCard(
                "IMS",
                VaadinIcon.USERS,
                I18n.t("landing.ims.title"),
                I18n.t("landing.ims.description"),
                new String[]{
                        I18n.t("landing.ims.feature.accounts"),
                        I18n.t("landing.ims.feature.tenants"),
                        I18n.t("landing.ims.feature.applications"),
                        I18n.t("landing.ims.feature.roles")
                },
                "ims",
                ButtonVariant.LUMO_SUCCESS
        ));

        return container;
    }

    private Div createCard(String shortName, VaadinIcon icon, String title, String description,
                           String[] features, String route, ButtonVariant buttonVariant) {
        Div card = new Div();
        card.addClassName("domain-card");
        card.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("box-shadow", "var(--lumo-box-shadow-xs)")
                .set("padding", "var(--lumo-space-l)")
                .set("width", "320px")
                .set("min-height", "360px")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("transition", "all 0.3s cubic-bezier(0.4, 0, 0.2, 1)")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("cursor", "pointer")
                .set("position", "relative")
                .set("overflow", "hidden");

        // Glass morphism effect
        card.getStyle()
                .set("backdrop-filter", "blur(10px)")
                .set("-webkit-backdrop-filter", "blur(10px)");

        // Accent color bar with gradient
        Div accentBar = new Div();
        String accentColor = route.equals("kms")
                ? "linear-gradient(90deg, var(--lumo-primary-color), #7c3aed)"
                : "linear-gradient(90deg, var(--lumo-success-color), #059669)";
        accentBar.getStyle()
                .set("position", "absolute")
                .set("top", "0")
                .set("left", "0")
                .set("right", "0")
                .set("height", "4px")
                .set("background", accentColor)
                .set("border-radius", "var(--lumo-border-radius-l) var(--lumo-border-radius-l) 0 0");

        card.add(accentBar);

        // Card content
        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(false);
        content.setAlignItems(FlexComponent.Alignment.CENTER);
        content.getStyle().set("flex", "1").set("padding-top", "var(--lumo-space-m)");

        // Badge / Short name with gradient
        Div badge = new Div(shortName);
        String badgeGradient = route.equals("kms")
                ? "linear-gradient(135deg, var(--lumo-primary-color-10pct), var(--lumo-primary-color-30pct))"
                : "linear-gradient(135deg, var(--lumo-success-color-10pct), var(--lumo-success-color-30pct))";
        badge.getStyle()
                .set("background", badgeGradient)
                .set("border-radius", "var(--lumo-border-radius-s)")
                .set("padding", "2px var(--lumo-space-s)")
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("font-weight", "700")
                .set("letter-spacing", "0.08em")
                .set("color", route.equals("kms") ? "var(--lumo-primary-color)" : "var(--lumo-success-color)")
                .set("margin-bottom", "var(--lumo-space-s)")
                .set("border", "1px solid " + (route.equals("kms") ? "var(--lumo-primary-color-30pct)" : "var(--lumo-success-color-30pct)"));

        // Icon with subtle glow
        Icon cardIcon = icon.create();
        cardIcon.setSize("48px");
        String iconColor = route.equals("kms") ? "var(--lumo-primary-color)" : "var(--lumo-success-color)";
        cardIcon.setColor(iconColor);
        cardIcon.getStyle()
                .set("margin-bottom", "var(--lumo-space-s)")
                .set("filter", "drop-shadow(0 4px 8px " + (route.equals("kms") ? "var(--lumo-primary-color-30pct)" : "var(--lumo-success-color-30pct)") + ")");

        // Title
        H2 cardTitle = new H2(title);
        cardTitle.getStyle()
                .set("font-size", "var(--lumo-font-size-l)")
                .set("font-weight", "600")
                .set("margin", "0 0 var(--lumo-space-xs)")
                .set("text-align", "center")
                .set("color", "var(--lumo-header-text-color)");

        // Description
        Paragraph desc = new Paragraph(description);
        desc.addClassName(LumoUtility.TextColor.SECONDARY);
        desc.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("text-align", "center")
                .set("margin", "0 0 var(--lumo-space-m)")
                .set("line-height", "1.6");

        // Features as chips with modern design
        Div featuresContainer = new Div();
        featuresContainer.getStyle()
                .set("display", "flex")
                .set("flex-wrap", "wrap")
                .set("gap", "var(--lumo-space-xs)")
                .set("justify-content", "center")
                .set("margin-bottom", "var(--lumo-space-m)")
                .set("width", "100%");

        for (String feature : features) {
            Div chip = new Div(feature);
            chip.addClassName("chip");
            chip.getStyle()
                    .set("background", "var(--lumo-contrast-5pct)")
                    .set("border-radius", "var(--lumo-border-radius-s)")
                    .set("padding", "4px var(--lumo-space-s)")
                    .set("font-size", "var(--lumo-font-size-xs)")
                    .set("color", "var(--lumo-secondary-text-color)")
                    .set("border", "1px solid var(--lumo-contrast-10pct)")
                    .set("transition", "all 0.2s ease");
            featuresContainer.add(chip);
        }

        // Enter button with modern styling
        Button enterBtn = new Button(I18n.t("landing." + route.toLowerCase() + ".button"), VaadinIcon.ARROW_RIGHT.create());
        enterBtn.addThemeVariants(buttonVariant);
        enterBtn.addClassName(LumoUtility.Width.FULL);
        enterBtn.getStyle()
                .set("margin-top", "auto")
                .set("font-weight", "600")
                .set("letter-spacing", "0.02em");

        // Click on card or button navigates
        Runnable navigate = () -> ui.navigate(route);
        enterBtn.addClickListener(e -> navigate.run());
        card.addClickListener(e -> navigate.run());

        content.add(badge, cardIcon, cardTitle, desc, featuresContainer, enterBtn);
        card.add(content);

        return card;
    }

    // ─── FOOTER ──────────────────────────────────────────────────────────────

    private Div buildFooter() {
        Div footer = new Div();
        footer.addClassName("landing-footer");
        footer.getStyle()
                .set("padding", "var(--lumo-space-m) 0")
                .set("text-align", "center")
                .set("border-top", "1px solid var(--lumo-contrast-10pct)")
                .set("background", "transparent")
                .set("flex-shrink", "0")
                .set("margin-top", "var(--lumo-space-l)")
                .set("width", "100%");

        Paragraph footerText = new Paragraph(I18n.t("landing.footer"));
        footerText.addClassName(LumoUtility.FontSize.XSMALL);
        footerText.addClassName(LumoUtility.TextColor.TERTIARY);
        footerText.getStyle().set("margin", "0");

        footer.add(footerText);
        return footer;
    }

    // ─── RESPONSIVE STYLES ──────────────────────────────────────────────────

    private void injectResponsiveStyles() {
        String css = """
                /* ── Reset & Base ── */
                .landing-view {
                    background: var(--lumo-shade-5pct);
                    min-height: 100vh;
                }

                /* ── Hero Section ── */
                .hero-section {
                    position: relative;
                    overflow: visible;
                }

                .hero-title {
                    display: inline-block;
                }

                /* Floating decorations */
                .floating-decor {
                    position: absolute;
                    opacity: 0.12;
                    pointer-events: none;
                    animation: float 8s ease-in-out infinite;
                }

                @keyframes float {
                    0%, 100% {
                        transform: translateY(0px) rotate(0deg);
                    }
                    25% {
                        transform: translateY(-15px) rotate(5deg);
                    }
                    75% {
                        transform: translateY(10px) rotate(-5deg);
                    }
                }

                .floating-decor:nth-child(2) {
                    animation-delay: 0.5s;
                }
                .floating-decor:nth-child(3) {
                    animation-delay: 1.2s;
                }
                .floating-decor:nth-child(4) {
                    animation-delay: 0.8s;
                }

                /* Hero underline glow */
                .hero-underline {
                    position: relative;
                }

                .hero-underline .glow {
                    position: absolute;
                    top: -2px;
                    left: -20px;
                    right: -20px;
                    height: 8px;
                    background: linear-gradient(90deg, transparent, var(--lumo-primary-color-30pct), var(--lumo-success-color-30pct), transparent);
                    filter: blur(8px);
                    border-radius: var(--lumo-border-radius-s);
                }

                /* ── Cards Container ── */
                .landing-view .cards-container {
                    gap: var(--lumo-space-l);
                }

                .landing-view .domain-card {
                    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
                }

                .landing-view .domain-card:hover {
                    transform: translateY(-8px);
                    box-shadow: var(--lumo-box-shadow-l);
                    border-color: var(--lumo-primary-color-30pct);
                }

                .landing-view .domain-card:active {
                    transform: scale(0.98);
                }

                .landing-view .domain-card vaadin-button {
                    width: 100%;
                }

                .landing-view .domain-card .chip:hover {
                    background: var(--lumo-contrast-10pct);
                    border-color: var(--lumo-primary-color-20pct);
                }

                /* ── Mobile First (default is mobile) ── */

                /* Small phones */
                @media (max-width: 480px) {
                    .landing-view .landing-main {
                        padding: var(--lumo-space-m);
                    }

                    .landing-view .domain-card {
                        width: 100%;
                        min-height: 280px;
                        padding: var(--lumo-space-m);
                    }

                    .landing-view .hero-title {
                        font-size: var(--lumo-font-size-xl) !important;
                    }

                    .landing-view .domain-card h2 {
                        font-size: var(--lumo-font-size-m) !important;
                    }

                    .landing-view .domain-card vaadin-icon {
                        width: 32px !important;
                        height: 32px !important;
                    }

                    .floating-decor {
                        display: none !important;
                    }

                    .hero-underline {
                        width: 80px !important;
                    }

                    .landing-view .landing-footer {
                        margin-top: var(--lumo-space-m);
                    }
                }

                /* Tablets and small desktops */
                @media (min-width: 481px) and (max-width: 768px) {
                    .landing-view .cards-container {
                        gap: var(--lumo-space-m);
                    }

                    .landing-view .domain-card {
                        width: 280px;
                        min-height: 300px;
                    }

                    .landing-view .hero-title {
                        font-size: var(--lumo-font-size-xxl) !important;
                    }

                    .floating-decor {
                        display: none !important;
                    }
                }

                /* Desktop */
                @media (min-width: 769px) {
                    .landing-view .cards-container {
                        gap: var(--lumo-space-xl);
                    }

                    .landing-view .domain-card {
                        width: 340px;
                        min-height: 380px;
                    }

                    .landing-view .domain-card:hover {
                        transform: translateY(-8px) scale(1.02);
                        box-shadow: var(--lumo-box-shadow-l);
                    }

                    .hero-title {
                        font-size: 3.5rem !important;
                    }
                }

                /* Large desktop */
                @media (min-width: 1200px) {
                    .hero-title {
                        font-size: 4rem !important;
                    }

                    .landing-view .domain-card {
                        width: 380px;
                        min-height: 400px;
                        padding: var(--lumo-space-xl);
                    }
                }

                /* ── Accessibility ── */
                .landing-view .domain-card:focus-visible {
                    outline: 2px solid var(--lumo-primary-color);
                    outline-offset: 2px;
                }

                .landing-view vaadin-button:focus-visible {
                    outline: 2px solid var(--lumo-primary-color);
                    outline-offset: 2px;
                }
                """;

        UI.getCurrent().getPage().executeJs(
                "const style = document.createElement('style'); style.textContent = $0; document.head.appendChild(style);",
                css
        );
    }

    // ─── HELPERS ─────────────────────────────────────────────────────────────

    private static class Span extends com.vaadin.flow.component.html.Span {
        public Span(String text) {
            super(text);
        }
    }
}
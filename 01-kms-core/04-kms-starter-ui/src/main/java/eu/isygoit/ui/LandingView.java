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
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.layout.BaseMainLayout;
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

    // ─── HERO SECTION ──────────────────────────────────────────────────────

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
        Div decorRight = createFloatingDecor(VaadinIcon.FILE_O, "var(--lumo-warning-color-30pct)", "100px", "15%");
        Div decorBottomLeft = createFloatingDecor(VaadinIcon.DATABASE, "var(--lumo-primary-color-20pct)", "-80px", "55%");
        Div decorBottomRight = createFloatingDecor(VaadinIcon.ENVELOPE, "var(--lumo-success-color-20pct)", "80px", "55%");

        hero.add(decorLeft, decorRight, decorBottomLeft, decorBottomRight);

        // ── Main Title ──
        Div titleContainer = new Div();
        titleContainer.getStyle()
                .set("position", "relative")
                .set("display", "inline-block")
                .set("margin-bottom", "var(--lumo-space-s)")
                .set("animation", "fadeInDown 0.8s ease-out");

        H1 headline = new H1();
        headline.addClassName("hero-title");
        headline.getStyle()
                .set("font-size", "var(--lumo-font-size-xxxl)")
                .set("font-weight", "800")
                .set("margin", "0")
                .set("line-height", "1.2")
                .set("letter-spacing", "-0.02em")
                .set("position", "relative")
                .set("z-index", "1")
                .set("color", "var(--lumo-header-text-color)");

        Text manageText = new Text(I18n.t("landing.title.main"));
        headline.add(manageText);

        // ── Glow effect ──
        Div glowEffect = new Div();
        glowEffect.addClassName("text-glow");
        glowEffect.getStyle()
                .set("position", "absolute")
                .set("top", "50%")
                .set("left", "50%")
                .set("transform", "translate(-50%, -50%)")
                .set("width", "100%")
                .set("height", "100%")
                .set("background", "radial-gradient(ellipse at center, var(--lumo-primary-color-10pct), transparent 70%)")
                .set("filter", "blur(30px)")
                .set("opacity", "0.4")
                .set("pointer-events", "none")
                .set("z-index", "0")
                .set("border-radius", "50%")
                .set("animation", "glowPulse 3s ease-in-out infinite");

        titleContainer.add(glowEffect);
        titleContainer.getElement().appendChild(headline.getElement());

        // ── Underline ──
        Div underline = new Div();
        underline.addClassName("hero-underline");
        underline.getStyle()
                .set("width", "140px")
                .set("height", "4px")
                .set("background", "linear-gradient(90deg, #4f46e5, #7c3aed, #059669, #d97706, #f472b6)")
                .set("border-radius", "var(--lumo-border-radius-s)")
                .set("margin", "var(--lumo-space-s) auto 0")
                .set("position", "relative")
                .set("animation", "slideIn 0.8s ease-out 0.3s both");

        Div glow = new Div();
        glow.getStyle()
                .set("position", "absolute")
                .set("top", "-2px")
                .set("left", "-20px")
                .set("right", "-20px")
                .set("height", "8px")
                .set("background", "linear-gradient(90deg, transparent, #4f46e5, #059669, #d97706, transparent)")
                .set("filter", "blur(8px)")
                .set("opacity", "0.5")
                .set("border-radius", "var(--lumo-border-radius-s)");
        underline.add(glow);

        // ── Subtitle ──
        HorizontalLayout subtitleLayout = new HorizontalLayout();
        subtitleLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        subtitleLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        subtitleLayout.setSpacing(true);
        subtitleLayout.getStyle()
                .set("margin-top", "var(--lumo-space-m)")
                .set("gap", "var(--lumo-space-s)")
                .set("animation", "fadeInUp 0.8s ease-out 0.5s both");

        Icon arrowIcon = VaadinIcon.ARROW_DOWN.create();
        arrowIcon.setSize("20px");
        arrowIcon.setColor("var(--lumo-secondary-text-color)");
        arrowIcon.getStyle().set("animation", "bounce 2s ease-in-out infinite");

        Paragraph subtitle = new Paragraph(I18n.t("landing.subtitle"));
        subtitle.addClassName(LumoUtility.TextColor.SECONDARY);
        subtitle.getStyle()
                .set("font-size", "var(--lumo-font-size-l)")
                .set("margin", "0")
                .set("font-weight", "400")
                .set("letter-spacing", "0.01em");

        subtitleLayout.add(arrowIcon, subtitle);

        hero.add(titleContainer, underline, subtitleLayout);

        return hero;
    }

    private Div createFloatingDecor(VaadinIcon icon, String color, String offsetX, String offsetY) {
        Div decor = new Div();
        decor.addClassName("floating-decor");
        decor.getStyle()
                .set("position", "absolute")
                .set("opacity", "0.12")
                .set("pointer-events", "none")
                .set("animation", "float 6s ease-in-out infinite")
                .set("animation-delay", Math.random() * 2 + "s");

        Icon decorIcon = icon.create();
        decorIcon.setSize("48px");
        decorIcon.setColor(color);

        decor.add(decorIcon);

        decor.getStyle()
                .set("left", offsetX)
                .set("top", offsetY)
                .set("transform", "translate(" + offsetX + ", " + offsetY + ")");

        return decor;
    }

    // ─── CARDS WITH ANIMATIONS ─────────────────────────────────────────────

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

        // KMS Card with animation delay
        Div kmsCard = createCard(
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
                ButtonVariant.LUMO_PRIMARY,
                "linear-gradient(135deg, #4f46e5, #7c3aed)",
                "#4f46e5",
                "rgba(79, 70, 229, 0.3)"
        );
        kmsCard.getStyle().set("animation", "cardFadeIn 0.6s ease-out 0.2s both");

        // IMS Card with animation delay
        Div imsCard = createCard(
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
                ButtonVariant.LUMO_SUCCESS,
                "linear-gradient(135deg, #059669, #10b981)",
                "#059669",
                "rgba(5, 150, 105, 0.3)"
        );
        imsCard.getStyle().set("animation", "cardFadeIn 0.6s ease-out 0.4s both");

        // MMS Card with animation delay
        Div mmsCard = createCard(
                "MMS",
                VaadinIcon.ENVELOPE,
                I18n.t("landing.mms.title"),
                I18n.t("landing.mms.description"),
                new String[]{
                        I18n.t("landing.mms.feature.senders"),
                        I18n.t("landing.mms.feature.templates"),
                        I18n.t("landing.mms.feature.email"),
                        I18n.t("landing.mms.feature.logs")
                },
                "mms",
                ButtonVariant.LUMO_WARNING,
                "linear-gradient(135deg, #d97706, #f59e0b)",
                "#d97706",
                "rgba(217, 119, 6, 0.3)"
        );
        mmsCard.getStyle().set("animation", "cardFadeIn 0.6s ease-out 0.6s both");

        // DMS Card with animation delay
        Div dmsCard = createCard(
                "DMS",
                VaadinIcon.FILE_O,
                I18n.t("landing.dms.title"),
                I18n.t("landing.dms.description"),
                new String[]{
                        I18n.t("landing.dms.feature.categories"),
                        I18n.t("landing.dms.feature.files"),
                        I18n.t("landing.dms.feature.tags"),
                        I18n.t("landing.dms.feature.versioning")
                },
                "dms",
                ButtonVariant.LUMO_ERROR,
                "linear-gradient(135deg, #db2777, #f472b6)",
                "#db2777",
                "rgba(219, 39, 119, 0.3)"
        );
        dmsCard.getStyle().set("animation", "cardFadeIn 0.6s ease-out 0.8s both");

        // SMS Card with animation delay
        Div smsCard = createCard(
                "SMS",
                VaadinIcon.DATABASE,
                I18n.t("landing.sms.title"),
                I18n.t("landing.sms.description"),
                new String[]{
                        I18n.t("landing.sms.feature.storage"),
                        I18n.t("landing.sms.feature.buckets"),
                        I18n.t("landing.sms.feature.usage"),
                        I18n.t("landing.sms.feature.types")
                },
                "sms",
                ButtonVariant.LUMO_CONTRAST,
                "linear-gradient(135deg, #1e293b, #475569)",
                "#1e293b",
                "rgba(30, 41, 59, 0.3)"
        );
        smsCard.getStyle().set("animation", "cardFadeIn 0.6s ease-out 1.0s both");

        // In the buildCardsContainer() method, add CMS card:

        Div cmsCard = createCard(
                "CMS",
                VaadinIcon.CALENDAR,
                I18n.t("landing.cms.title"),
                I18n.t("landing.cms.description"),
                new String[]{
                        I18n.t("landing.cms.feature.calendars"),
                        I18n.t("landing.cms.feature.events"),
                        I18n.t("landing.cms.feature.schedules"),
                        I18n.t("landing.cms.feature.ics")
                },
                "cms",
                ButtonVariant.LUMO_PRIMARY,
                "linear-gradient(135deg, #2563eb, #3b82f6)",
                "#2563eb",
                "rgba(37, 99, 235, 0.3)"
        );
        cmsCard.getStyle().set("animation", "cardFadeIn 0.6s ease-out 1.2s both");

        // Organize cards in a responsive grid - 5 cards in a row on large screens
        container.add(kmsCard, imsCard, mmsCard, dmsCard, smsCard, cmsCard);

        return container;
    }

    private Div createCard(String shortName, VaadinIcon icon, String title, String description,
                           String[] features, String route, ButtonVariant buttonVariant,
                           String gradient, String color, String shadowColor) {
        Div card = new Div();
        card.addClassName("domain-card");
        card.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("box-shadow", "var(--lumo-box-shadow-xs)")
                .set("padding", "var(--lumo-space-l)")
                .set("width", "260px")
                .set("min-height", "340px")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("transition", "all 0.3s cubic-bezier(0.4, 0, 0.2, 1)")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("cursor", "pointer")
                .set("position", "relative")
                .set("overflow", "hidden")
                .set("backdrop-filter", "blur(10px)")
                .set("-webkit-backdrop-filter", "blur(10px)")
                .set("transform", "translateY(30px)")
                .set("opacity", "0");

        // ── Accent color bar with gradient and shimmer ──
        Div accentBar = new Div();
        accentBar.getStyle()
                .set("position", "absolute")
                .set("top", "0")
                .set("left", "0")
                .set("right", "0")
                .set("height", "4px")
                .set("background", gradient)
                .set("border-radius", "var(--lumo-border-radius-l) var(--lumo-border-radius-l) 0 0")
                .set("background-size", "200% 100%")
                .set("animation", "shimmer 3s ease-in-out infinite");

        card.add(accentBar);

        // ── Card glow effect on hover ──
        Div cardGlow = new Div();
        cardGlow.addClassName("card-glow");
        cardGlow.getStyle()
                .set("position", "absolute")
                .set("top", "-50%")
                .set("left", "-50%")
                .set("width", "200%")
                .set("height", "200%")
                .set("background", "radial-gradient(circle, " + shadowColor.replace("0.3", "0.03") + ", transparent 70%)")
                .set("pointer-events", "none")
                .set("transition", "all 0.6s ease")
                .set("transform", "scale(0.8)")
                .set("opacity", "0");

        card.add(cardGlow);

        // ── Card content ──
        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(false);
        content.setAlignItems(FlexComponent.Alignment.CENTER);
        content.getStyle().set("flex", "1").set("padding-top", "var(--lumo-space-m)");

        // Badge / Short name
        Div badge = new Div(shortName);
        badge.getStyle()
                .set("background", gradient)
                .set("-webkit-background-clip", "text")
                .set("-webkit-text-fill-color", "transparent")
                .set("background-clip", "text")
                .set("border-radius", "var(--lumo-border-radius-s)")
                .set("padding", "2px var(--lumo-space-s)")
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("font-weight", "700")
                .set("letter-spacing", "0.08em")
                .set("margin-bottom", "var(--lumo-space-s)")
                .set("border", "1px solid " + color)
                .set("background-color", "var(--lumo-base-color)");

        // Icon with pulse animation
        Icon cardIcon = icon.create();
        cardIcon.setSize("36px");
        cardIcon.setColor(color);
        cardIcon.getStyle()
                .set("margin-bottom", "var(--lumo-space-s)")
                .set("filter", "drop-shadow(0 4px 8px " + shadowColor + ")")
                .set("animation", "iconPulse 3s ease-in-out infinite");

        // Title
        H2 cardTitle = new H2(title);
        cardTitle.getStyle()
                .set("font-size", "var(--lumo-font-size-m)")
                .set("font-weight", "600")
                .set("margin", "0 0 var(--lumo-space-xs)")
                .set("text-align", "center")
                .set("color", "var(--lumo-header-text-color)");

        // Description
        Paragraph desc = new Paragraph(description);
        desc.addClassName(LumoUtility.TextColor.SECONDARY);
        desc.getStyle()
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("text-align", "center")
                .set("margin", "0 0 var(--lumo-space-s)")
                .set("line-height", "1.5");

        // Features as chips with stagger animation
        Div featuresContainer = new Div();
        featuresContainer.getStyle()
                .set("display", "flex")
                .set("flex-wrap", "wrap")
                .set("gap", "var(--lumo-space-xs)")
                .set("justify-content", "center")
                .set("margin-bottom", "var(--lumo-space-m)")
                .set("width", "100%");

        int chipDelay = 0;
        for (String feature : features) {
            Div chip = new Div(feature);
            chip.addClassName("chip");
            chip.getStyle()
                    .set("background", "var(--lumo-contrast-5pct)")
                    .set("border-radius", "var(--lumo-border-radius-s)")
                    .set("padding", "2px var(--lumo-space-s)")
                    .set("font-size", "var(--lumo-font-size-xs)")
                    .set("color", "var(--lumo-secondary-text-color)")
                    .set("border", "1px solid var(--lumo-contrast-10pct)")
                    .set("transition", "all 0.2s ease")
                    .set("animation", "chipFadeIn 0.4s ease-out " + (0.6 + chipDelay * 0.1) + "s both");
            featuresContainer.add(chip);
            chipDelay++;
        }

        // Enter button
        Button enterBtn = new Button(I18n.t("landing." + route.toLowerCase() + ".button"), VaadinIcon.ARROW_RIGHT.create());
        enterBtn.addThemeVariants(buttonVariant);
        enterBtn.addClassName(LumoUtility.Width.FULL);
        enterBtn.getStyle()
                .set("margin-top", "auto")
                .set("font-weight", "600")
                .set("letter-spacing", "0.02em")
                .set("transition", "all 0.3s ease")
                .set("position", "relative")
                .set("overflow", "hidden");

        // Button click - navigate
        enterBtn.addClickListener(e -> {
            if (ui != null) {
                ui.navigate(route);
            } else {
                UI.getCurrent().navigate(route);
            }
        });

        // Click on card navigates
        card.addClickListener(e -> {
            // Add click animation
            card.getStyle().set("transform", "scale(0.95)");
            UI.getCurrent().getPage().executeJs(
                    "setTimeout(() => { $0.style.transform = 'scale(1)'; }, 150)",
                    card.getElement()
            );
            if (ui != null) {
                ui.navigate(route);
            } else {
                UI.getCurrent().navigate(route);
            }
        });

        // Hover effects
        card.addAttachListener(e -> {
            card.getElement().executeJs(
                    "this.addEventListener('mouseenter', function() {" +
                            "  this.querySelector('.card-glow').style.opacity = '1';" +
                            "  this.querySelector('.card-glow').style.transform = 'scale(1)';" +
                            "});" +
                            "this.addEventListener('mouseleave', function() {" +
                            "  this.querySelector('.card-glow').style.opacity = '0';" +
                            "  this.querySelector('.card-glow').style.transform = 'scale(0.8)';" +
                            "});"
            );
        });

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
                .set("width", "100%")
                .set("animation", "fadeInUp 0.8s ease-out 0.8s both");

        Paragraph footerText = new Paragraph(I18n.t("landing.footer"));
        footerText.addClassName(LumoUtility.FontSize.XSMALL);
        footerText.addClassName(LumoUtility.TextColor.TERTIARY);
        footerText.getStyle().set("margin", "0");

        footer.add(footerText);
        return footer;
    }

    // ─── RESPONSIVE STYLES WITH ANIMATIONS ─────────────────────────────────

    private void injectResponsiveStyles() {
        String css = """
                /* ── Reset & Base ── */
                .landing-view {
                    background: var(--lumo-shade-5pct);
                    min-height: 100vh;
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                    justify-content: center;
                }
                
                .landing-main {
                    flex: 1;
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                    justify-content: center;
                    padding: var(--lumo-space-l) var(--lumo-space-m);
                    max-width: 1200px;
                    width: 100%;
                    margin: 0 auto;
                }
                
                /* ── Keyframe Animations ── */
                @keyframes fadeInDown {
                    from {
                        opacity: 0;
                        transform: translateY(-30px);
                    }
                    to {
                        opacity: 1;
                        transform: translateY(0);
                    }
                }
                
                @keyframes fadeInUp {
                    from {
                        opacity: 0;
                        transform: translateY(30px);
                    }
                    to {
                        opacity: 1;
                        transform: translateY(0);
                    }
                }
                
                @keyframes slideIn {
                    from {
                        width: 0;
                        opacity: 0;
                    }
                    to {
                        width: 140px;
                        opacity: 1;
                    }
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
                
                @keyframes glowPulse {
                    0%, 100% {
                        opacity: 0.3;
                        transform: translate(-50%, -50%) scale(1);
                    }
                    50% {
                        opacity: 0.6;
                        transform: translate(-50%, -50%) scale(1.1);
                    }
                }
                
                @keyframes bounce {
                    0%, 100% {
                        transform: translateY(0);
                    }
                    50% {
                        transform: translateY(8px);
                    }
                }
                
                @keyframes shimmer {
                    0% {
                        background-position: -200% 0;
                    }
                    100% {
                        background-position: 200% 0;
                    }
                }
                
                @keyframes iconPulse {
                    0%, 100% {
                        transform: scale(1);
                    }
                    50% {
                        transform: scale(1.08);
                    }
                }
                
                @keyframes cardFadeIn {
                    from {
                        opacity: 0;
                        transform: translateY(30px) scale(0.95);
                    }
                    to {
                        opacity: 1;
                        transform: translateY(0) scale(1);
                    }
                }
                
                @keyframes chipFadeIn {
                    from {
                        opacity: 0;
                        transform: scale(0.8);
                    }
                    to {
                        opacity: 1;
                        transform: scale(1);
                    }
                }
                
                /* ── Hero Section ── */
                .hero-section {
                    position: relative;
                    overflow: visible;
                }
                
                .hero-title {
                    display: inline-block;
                    position: relative;
                    z-index: 1;
                }
                
                .text-glow {
                    animation: glowPulse 3s ease-in-out infinite;
                }
                
                .floating-decor {
                    position: absolute;
                    opacity: 0.12;
                    pointer-events: none;
                    animation: float 8s ease-in-out infinite;
                }
                
                .floating-decor:nth-child(1) { animation-delay: 0s; }
                .floating-decor:nth-child(2) { animation-delay: 0.5s; }
                .floating-decor:nth-child(3) { animation-delay: 1.2s; }
                .floating-decor:nth-child(4) { animation-delay: 0.8s; }
                
                .hero-underline {
                    position: relative;
                }
                
                /* ── Cards ── */
                .cards-container {
                    gap: var(--lumo-space-l);
                }
                
                .domain-card {
                    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
                    position: relative;
                    overflow: hidden;
                }
                
                .domain-card:hover {
                    transform: translateY(-8px) !important;
                    box-shadow: var(--lumo-box-shadow-l);
                    border-color: var(--lumo-primary-color-30pct);
                }
                
                .domain-card:active {
                    transform: scale(0.98) !important;
                }
                
                .domain-card vaadin-button {
                    width: 100%;
                }
                
                .domain-card .chip {
                    transition: all 0.2s ease;
                }
                
                .domain-card .chip:hover {
                    background: var(--lumo-contrast-10pct);
                    border-color: var(--lumo-primary-color-20pct);
                    transform: translateY(-2px);
                }
                
                /* ── Mobile First (default is mobile) ── */
                @media (max-width: 480px) {
                    .landing-main {
                        padding: var(--lumo-space-m);
                    }
                
                    .domain-card {
                        width: 100% !important;
                        min-height: 280px;
                        padding: var(--lumo-space-m);
                    }
                
                    .hero-title {
                        font-size: var(--lumo-font-size-xl) !important;
                    }
                
                    .domain-card h2 {
                        font-size: var(--lumo-font-size-m) !important;
                    }
                
                    .domain-card vaadin-icon {
                        width: 32px !important;
                        height: 32px !important;
                    }
                
                    .floating-decor {
                        display: none !important;
                    }
                
                    .hero-underline {
                        width: 80px !important;
                    }
                
                    .landing-footer {
                        margin-top: var(--lumo-space-m);
                    }
                
                    .text-glow {
                        display: none !important;
                    }
                
                    .hero-section {
                        padding: var(--lumo-space-s) 0 !important;
                    }
                
                    @keyframes slideIn {
                        from {
                            width: 0;
                            opacity: 0;
                        }
                        to {
                            width: 80px;
                            opacity: 1;
                        }
                    }
                }
                
                /* Tablets */
                @media (min-width: 481px) and (max-width: 768px) {
                    .cards-container {
                        gap: var(--lumo-space-m);
                    }
                
                    .domain-card {
                        width: 240px;
                        min-height: 280px;
                        padding: var(--lumo-space-m);
                    }
                
                    .hero-title {
                        font-size: var(--lumo-font-size-xxl) !important;
                    }
                
                    .floating-decor {
                        display: none !important;
                    }
                }
                
                /* Desktop */
                @media (min-width: 769px) and (max-width: 1024px) {
                    .cards-container {
                        gap: var(--lumo-space-m);
                    }
                
                    .domain-card {
                        width: 200px;
                        min-height: 300px;
                        padding: var(--lumo-space-m);
                    }
                
                    .domain-card h2 {
                        font-size: var(--lumo-font-size-s) !important;
                    }
                
                    .domain-card .chip {
                        font-size: var(--lumo-font-size-xxs) !important;
                    }
                }
                
                /* Large desktop */
                @media (min-width: 1025px) {
                    .cards-container {
                        gap: var(--lumo-space-l);
                    }
                
                    .domain-card {
                        width: 220px;
                        min-height: 320px;
                        padding: var(--lumo-space-l);
                    }
                
                    .domain-card:hover {
                        transform: translateY(-8px) scale(1.02);
                        box-shadow: var(--lumo-box-shadow-l);
                    }
                
                    .hero-title {
                        font-size: 3.5rem !important;
                    }
                }
                
                /* Extra large desktop */
                @media (min-width: 1400px) {
                    .domain-card {
                        width: 240px;
                        min-height: 340px;
                        padding: var(--lumo-space-l);
                    }
                }
                
                /* ── Accessibility ── */
                .domain-card:focus-visible {
                    outline: 2px solid var(--lumo-primary-color);
                    outline-offset: 2px;
                }
                
                vaadin-button:focus-visible {
                    outline: 2px solid var(--lumo-primary-color);
                    outline-offset: 2px;
                }
                
                /* Reduced motion preference */
                @media (prefers-reduced-motion: reduce) {
                    *,
                    *::before,
                    *::after {
                        animation-duration: 0.01ms !important;
                        animation-iteration-count: 1 !important;
                        transition-duration: 0.01ms !important;
                    }
                
                    .domain-card {
                        opacity: 1 !important;
                        transform: none !important;
                    }
                
                    .domain-card:hover {
                        transform: none !important;
                    }
                
                    .floating-decor {
                        display: none !important;
                    }
                }
                """;

        UI.getCurrent().getPage().executeJs(
                "const style = document.createElement('style'); style.textContent = $0; document.head.appendChild(style);",
                css
        );
    }
}